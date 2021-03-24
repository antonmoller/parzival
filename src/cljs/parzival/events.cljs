(ns parzival.events
  (:require
   [re-frame.core :as re-frame :refer [dispatch reg-event-fx reg-cofx reg-fx inject-cofx on-changes after]]
   [parzival.db :as db]
   ["pdfjs-dist" :as pdfjs]
   ["pdfjs-dist/web/pdf_viewer.js" :as pdfjs-viewer]
   [goog :as goog]
   [goog.object :as obj]
   [cljs.core.async :refer [go go-loop <! timeout]]
   [cljs.core.async.interop :refer [<p!]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(obj/set (obj/getValueByKeys pdfjs "GlobalWorkerOptions") "workerSrc" "/js/compiled/pdf.worker.js")

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 :left-sidebar/toggle
 (fn [db _]
   (update db :left-sidebar/open not)))

(re-frame/reg-event-db
 :right-sidebar/toggle
 (fn [db _]
   (update db :right-sidebar/open not)))

(re-frame/reg-event-db
 :right-sidebar/set-width
 (fn [db [_ width]]
   (assoc db :right-sidebar/width width)))

(re-frame/reg-event-db
 :settings/toggle
 (fn [db _]
   (update db :settings/open not)))

(re-frame/reg-event-db
 :search/toggle
 (fn [db _]
   (update db :search/open not)))

(re-frame/reg-event-db
 :theme/switch
 (fn [db _]
   (update db :theme/dark not)))

(re-frame/reg-event-db
 :loading/progress
 (fn [db prog]
   (assoc db :loading/progress prog)))

(re-frame/reg-event-db
  :pdf/load-success
  (fn [db [_ pdf]]
    (assoc db :pdf pdf)))

;TODO: Change error handling so it's actually useful
; Is this called everytime :pdf is called????
(reg-event-fx
  :pdf/load-failure
  (fn [_ [_ err]]
    (js/console.log err)))

(reg-fx
  :pdf/document
  (fn [{:keys [url on-success on-failure]}]
    (go
      (try
        (let [pdf (<p! (obj/get (.getDocument pdfjs (js-obj "url" url 
                                                            "cMapUrl" "../../node_modules/pdfjs-dist/cmaps/" 
                                                            "cMapPacked" true
                                                            )) "promise"))]
          (dispatch (conj on-success pdf)))
        (catch js/Error e (dispatch (conj on-failure (ex-cause e))))))))

(reg-event-fx
  :pdf/load
  (fn [_ [_ url]]
    {:pdf/document {:url url
                    :on-success [:pdf/load-success]
                    :on-failure [:pdf/load-failure]}}))

(defn text
  [node]
  (obj/get node "firstChild"))

(defn length
  [node]
  (obj/getValueByKeys node "firstChild" "length"))

(defn calc-coord
  [viewport page-rect rect]
  (.concat ^js (.convertToPdfPoint viewport (- (obj/get rect "left") (obj/get page-rect "x"))
                                            (- (obj/get rect "top")  (obj/get page-rect "y")))
           ^js (.convertToPdfPoint viewport (- (obj/get rect "right") (obj/get page-rect "x"))
                                            (- (obj/get rect "bottom")  (obj/get page-rect "y")))))

(defn get-end
  [range-obj]
  (let [end (obj/get range-obj "endContainer")]
    (if (= (obj/get end "nodeName") "SPAN")
      [(obj/get end "previousSibling") 
       (obj/getValueByKeys end "previousSibling" "lastChild" "length")]
      [(obj/get end "parentNode")
       (obj/get range-obj "endOffset")])))

(defn render-highlight
  [{:keys [color start-id end-id start-offset end-offset]} page]
  (let [text-layer (obj/getValueByKeys page "textLayer" "textLayerDiv")
        rows (obj/get text-layer "children")
        fragment (js/DocumentFragment.)
        parent (.createElement js/document "div")
        viewport (obj/get page "viewport")
        page-rect (aget (.getClientRects (obj/get page "canvas")) 0)
        r (js/Range.)]
    (.setAttribute parent "style" "cursor: pointer; position: absolute;")
    (.append fragment parent)
    (doseq [i (range start-id (inc end-id))]
      (.setStart r (text (.item rows i)) (if (== i start-id) start-offset 0))
      (.setEnd r (text (.item rows i)) (if (== i end-id) end-offset (length (.item rows i))))
      (let [ [b0 b1 b2 b3] (->> (.getBoundingClientRect r)
                                (calc-coord viewport page-rect)
                                ^js (.convertToViewportRectangle viewport))
            child (.createElement js/document "div")]
        (.setAttribute child "style" (str "position: absolute; background-color: " color
                                          "; left: " (min b0 b2) "px; top: " (min b1 b3) 
                                          "px; width: " (Math/abs (- b0 b2)) 
                                          "px; height: " (Math/abs (- b1 b3)) "px;"))
        (.append parent child)))
    (.append text-layer fragment)))

(reg-event-fx
  :render/page
  (fn [{:keys [db]} [_ page-id]]
    (let [page (.getPageView (get db :pdf/viewer) page-id)]
      (run! #(render-highlight (second %) page) (get-in db [:pdf/highlights page-id])))))

(reg-event-fx
  :highlight
  (fn [{:keys [db]} [_ color _ id]]
    (let [range-obj (.getRangeAt (.getSelection js/document) 0)] 
      (when-not (obj/get range-obj "collapsed")
        (let [text-layer (obj/getValueByKeys range-obj "startContainer" "parentNode" "parentNode")
              page-id (-> (obj/get text-layer "parentNode")
                          (.getAttribute "data-page-number")
                          (dec))
              page    (.getPageView (get db :pdf/viewer) page-id)
              text-rows (.from js/Array (obj/get text-layer "children"))
              [end end-offset] (get-end range-obj)
              highlight {:color color
                         :start-id (.indexOf text-rows (obj/getValueByKeys range-obj "startContainer" "parentNode"))
                         :end-id (.indexOf text-rows end)
                         :start-offset (obj/get range-obj "startOffset")
                         :end-offset end-offset}]
          (.collapse range-obj)
          {:db (if-let [page-highlights (get-in db [:pdf/highlights page-id])]
                 (update-in db [:pdf/highlights page-id] assoc (count page-highlights) highlight)
                 (assoc-in db [:pdf/highlights page-id 0] highlight))
           :fx [(render-highlight highlight page)]})))))

(reg-event-fx
 :pdf/view
 (fn [{:keys [db]} _]
  (let [pdf (get db :pdf)
        container (.getElementById js/document "viewerContainer")
        event-bus (pdfjs-viewer/EventBus.)
        link-service (pdfjs-viewer/PDFLinkService. (js-obj "eventBus" event-bus "externalLinkTarget" 2))
        ; find-controller (pdfjs-viewer/PDFFindController. (js-obj "eventBus" event-bus "linkService" link-service))
        pdf-viewer (pdfjs-viewer/PDFViewer. (js-obj "container" container 
                                                    "eventBus" event-bus 
                                                    "linkService" link-service
                                                    ; "findController" find-controller
                                                    "textLayerMode" 2))]
  (.setViewer link-service pdf-viewer)
  (.setDocument pdf-viewer pdf)
  (.setDocument link-service pdf nil)
  (.on event-bus "textlayerrendered" 
                 #(dispatch [:render/page (dec (obj/get % "pageNumber"))]))
  {:db (assoc db :pdf/viewer pdf-viewer)})))


; (reg-event-fx
;   :resize
;   (let [handle-mousemove (fn [e])
;         handle-mouseup   (fn [])]
;     (.addEventListener js/document "mousemove" handle-mousemove)
;     (.addEventListener js/document "mouseup" handle-mouseup)

        

; ;; Pagemarks
(reg-event-fx 
  :pagemark
  (fn [{:keys [db]} _] 
    (let [text-layer (-> (get db :pdf/viewer)
                         (.getPageView 0)
                         (obj/getValueByKeys "textLayer" "textLayerDiv"))
          fragment (js/DocumentFragment.)
          outer (.createElement js/document "div")
          inner (.createElement js/document "div")

          ; parent (.createElement js/document "div")
          ; left   (.createElement js/document "div")
          ; right  (.createElement js/document "div")
          ; top    (.createElement js/document "div")
          ; bottom (.createElement js/document "div")
          ]
      (js/console.log text-layer)
      (.setAttribute outer "style" "position: absolute; left 0; top: 0;")
       (.setAttribute inner "style" "cursor: ew-resize; border: 4px solid blue;
                                     width: 4px; height: 600px; width: 500px;")
      (.append fragment outer)
      (.append outer inner)
      (.append text-layer fragment)
      ; (.append fragment parent)
       ; (.setAttribute left "style" "cursor: ew-resize; position: absolute;
       ;                             left: 0; top: 0; width: 4px; height: 600px;
       ;                             background-color: rgba(0,0,255,1);")
       ; (.setAttribute right "style" "cursor: ew-resize; position: absolute;
       ;                             left: 600px; top: 0; width: 4px; height: 600px;
       ;                             background-color: rgba(0,0,255,1);")
       ; (.setAttribute top "style" "cursor: ns-resize; position: absolute;
       ;                             left: 4px; top: 0; width: 596px; height: 4px;
       ;                             background-color: rgba(0,0,255,1);")
       ; (.setAttribute bottom "style" "cursor: ns-resize; position: absolute;
       ;                             left: 4px; top: 596px; width: 596px; height: 4px;
       ;                             background-color: rgba(0,0,255,1);")
       ; (.append parent left)
       ; (.append parent right)
       ; (.append parent top)
       ; (.append parent bottom)
       ; (.append text-layer fragment)





      )))







