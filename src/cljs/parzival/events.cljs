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
  [rect page-rect viewport]
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

(defn get-highlight
  [range-obj page-rect viewport color]
  (let [start-row            (obj/getValueByKeys range-obj "startContainer" "parentNode")
        start-offset         (obj/get range-obj "startOffset")
        [end-row end-offset] (get-end range-obj)
        r                    (js/Range.)]
    (loop [row start-row
           coords []]
      (if (or (nil? row) (.isSameNode row (obj/get end-row "nextSibling")))
        {:color color :coords coords}
        (do 
          (.setStart r (text row) (if (.isSameNode row start-row) start-offset 0))
          (.setEnd r (text row) (if (.isSameNode row end-row) end-offset (length row)))
          (recur (obj/get row "nextSibling")
                 (conj coords
                       (-> (.getBoundingClientRect r)
                           (calc-coord page-rect viewport)))))))))

(defn render-highlight
  [{:keys [color coords]} viewport text-layer]
  (let [fragment (js/DocumentFragment.)
        parent (.createElement js/document "div")]
    (.setAttribute parent "style" "cursor: pointer; position: absolute; mix-blend-mode: multiply;")
    (.append fragment parent)
    (doseq [rect coords]
      (let [[b0 b1 b2 b3] ^js (.convertToViewportRectangle viewport rect)
            child (.createElement js/document "div")]
        (.setAttribute child "style" (str "position: absolute; background-color: " color
                                          "; left: " (min b0 b2) "px; top: " (min b1 b3) 
                                          "px; width: " (Math/abs (- b0 b2)) 
                                          "px; height: " (Math/abs (- b1 b3)) "px;"))
        (.append parent child)))
    (.append text-layer fragment)))

(reg-event-fx
  :render/page
  (fn [{:keys [db]} [_ page-id text-layer]]
    (let [viewport (-> (get db :pdf/viewer)
                       (.getPageView page-id)
                       (obj/get "viewport"))]
      (run! #(render-highlight (second %) viewport text-layer) (get-in db [:pdf/highlights page-id])))))

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
              viewport (obj/get page "viewport")
              page-rect (aget (.getClientRects (obj/get page "canvas")) 0)
              highlight (get-highlight range-obj page-rect viewport color)]
          (render-highlight highlight viewport text-layer)
          (.collapse range-obj)
          {:db (if-let [page-highlights (get-in db [:pdf/highlights page-id])]
                 (update-in db [:pdf/highlights page-id] assoc (count page-highlights) highlight)
                 (assoc-in db [:pdf/highlights page-id 0] highlight))
           :fx [(render-highlight highlight viewport text-layer)]})))))

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
                 #(dispatch [:render/page (dec (obj/get % "pageNumber"))
                                          (obj/getValueByKeys % "source" "textLayerDiv")]))
  {:db (assoc db :pdf/viewer pdf-viewer)})))
