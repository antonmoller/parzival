(ns parzival.pdf
  (:require
   [re-frame.core :as rf :refer [dispatch reg-event-fx reg-cofx reg-fx  inject-cofx on-changes after]]
   [parzival.db :as db]
   ["pdfjs-dist" :as pdfjs]
   ["pdfjs-dist/web/pdf_viewer.js" :as pdfjs-viewer]
   [cljs.core.async :refer [go go-loop <! timeout]]
   [cljs.core.async.interop :refer [<p!]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))


;; Subs

(rf/reg-sub
  :pdf?
  (fn [db _]
    (some? (get db :pdf))))

(rf/reg-sub
 :highlight/open
 (fn [db _]
   (get db :highlight/open)))

(rf/reg-sub
 :highlight/anchor
 (fn [db _]
   (get db :highlight/anchor)))


;;; Events

(rf/reg-event-db
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
        (let [pdf (<p! (.-promise (.getDocument pdfjs (js-obj "url" url 
                                                              "cMapUrl" "../../node_modules/pdfjs-dist/cmaps/" 
                                                              "cMapPacked" true))))]
          (dispatch (conj on-success pdf)))
        (catch js/Error e (dispatch (conj on-failure (ex-cause e))))))))

(reg-event-fx
  :pdf/load
  (fn [_ [_ url]]
    (set! (.. pdfjs -GlobalWorkerOptions -workerSrc) "/js/compiled/pdf.worker.js")
    {:pdf/document {:url url
                    :on-success [:pdf/load-success]
                    :on-failure [:pdf/load-failure]}}))

(defn text
  [node]
  (.-firstChild node))

(defn length
  [node]
  (.. node -firstChild -length))

(defn calc-coord
  [viewport page-rect rect]
  (.concat ^js (.convertToPdfPoint viewport (- (.-left rect)   (.-x page-rect))
                                            (- (.-top rect)    (.-y page-rect)))
           ^js (.convertToPdfPoint viewport (- (.-right rect)  (.-x page-rect))
                                            (- (.-bottom rect) (.-y page-rect)))))

(defn get-end
  [range-obj]
  (let [end (.-endContainer range-obj)]
    (if (= (.-nodeName end) "SPAN")
      [(.-previousSibling end) 
       (.. end -previousSibling -lastChild -length)]
      [(.-parentNode end)
       (.-endOffset range-obj)])))

(defn render-highlight
  [{:keys [color start-id end-id start-offset end-offset]} page]
  (let [text-layer (.. page -textLayer -textLayerDiv)
        rows (.-children text-layer)
        fragment (js/DocumentFragment.)
        parent (.createElement js/document "div")
        viewport (.-viewport page)
        page-rect (-> (.-canvas page)
                       (.getClientRects)
                       (aget 0))
        r (js/Range.)]
    (.setAttribute parent "style" "cursor: pointer; position: absolute;")
    (.append fragment parent)
    (doseq [i (range start-id (inc end-id))]
      (.setStart r (text (.item rows i)) (if (== i start-id) start-offset 0))
      (.setEnd r (text (.item rows i)) (if (== i end-id) end-offset (length (.item rows i))))
      (let [[b0 b1 b2 b3] (->> (.getBoundingClientRect r)
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

;; (reg-event-fx
;;   :highlight
;;   (fn [{:keys [db]} [_ color _ id]]
;;     (let [range-obj (.getRangeAt (.getSelection js/document) 0)] 
;;       (when-not (.-collapsed range-obj)
;;         (let [text-layer (.. range-obj -startContainer -parentNode -parentNode)
;;               page-id (-> (.-parentNode text-layer)
;;                           (.getAttribute "data-page-number")
;;                           (dec))
;;               page    (.getPageView (get db :pdf/viewer) page-id)
;;               text-rows (.from js/Array (.-children text-layer))
;;               [end end-offset] (get-end range-obj)
;;               highlight {:color color
;;                          :start-id (.indexOf text-rows (.. range-obj -startContainer -parentNode))
;;                          :end-id (.indexOf text-rows end)
;;                          :start-offset (.-startOffset range-obj)
;;                          :end-offset end-offset}]
;;           (.collapse range-obj)
;;           {:db (if-let [page-highlights (get-in db [:pdf/highlights page-id])]
;;                  (update-in db [:pdf/highlights page-id] assoc (count page-highlights) highlight)
;;                  (assoc-in db [:pdf/highlights page-id 0] highlight))
;;            :fx [(render-highlight highlight page)]})))))

(defn valid-range?
  []
  (let [range-obj (.getRangeAt (.getSelection js/document) 0)]
    (when-not (.-collapsed range-obj)
      (let [text-layer-start (.. range-obj -startContainer -parentNode -parentNode)
            text-layer-end   (.. range-obj -endContainer -parentNode -parentNode)]
            (.isSameNode text-layer-start text-layer-end)))))
           
(reg-event-fx
 :highlight
 (fn [{:keys [db]} _]
   (when (valid-range?)
     (js/console.log true))))

;;          text-layer-end   (.. range-obj -startContanier -parentNode -parentNode)]
;;      (when (and (not (.-collapsed range-obj)) (.isSameNode text-layer-start text-layer-end))
;;        (js/console.log "HI THERE IT'S WORKING"))
;;      ))
;;  )

(reg-event-fx
 :pdf/view
 (fn [{:keys [db]} _]
  (let [pdf (get db :pdf)
        container (.getElementById js/document "viewerContainer")
        viewer    (.getElementById js/document "viewer")
        event-bus (pdfjs-viewer/EventBus.)
        link-service (pdfjs-viewer/PDFLinkService. (js-obj "eventBus" event-bus "externalLinkTarget" 2))
        ; find-controller (pdfjs-viewer/PDFFindController. (js-obj "eventBus" event-bus "linkService" link-service))
        pdf-viewer (pdfjs-viewer/PDFViewer. (js-obj "container" container 
                                                    "viewer" viewer 
                                                    "eventBus" event-bus 
                                                    "linkService" link-service
                                                    ; "findController" find-controller
                                                    "textLayerMode" 2))]
  (.setViewer link-service pdf-viewer)
  (.setDocument pdf-viewer pdf)
  (.setDocument link-service pdf nil)
  (.on event-bus "textlayerrendered" 
                 #(dispatch [:render/page (dec (.-pageNumber %))]))
  {:db (assoc db :pdf/viewer pdf-viewer)})))


; (reg-event-fx
;   :resize
;   (let [handle-mousemove (fn [e])
;         handle-mouseup   (fn [])]
;     (.addEventListener js/document "mousemove" handle-mousemove)
;     (.addEventListener js/document "mouseup" handle-mouseup)

        
(defn pagemark-vertical-resize
  [])

; (defn pagemark-horizontal-resize
;   []
;   (let [handle-mousemove (fn [e]
;                            (.. e preventDefault)
;                            (let [x (.-clientX e)
;                                  inner-w js/window.innerWidth
;                                  new-width (-> (- inner-w x)
;                                                (/ inner-w)
;                                                (* 100))]
;                              (.setAttribute
;   (.addEventListener "mousemove" handle-mousemove)
;   (.addEventListener "mouseup" handle-mouseup)

  ;;FIXME: In handle-mouseup
  ;(.removeEventListener "mousemove" handle-mousemove)p
  ;(.removeEventListener "mouseup" handle-mouseup)
  ;)

(defn horizontal-resize
  [element event]
  (let [style (.-style element)
        handle-mousemove (fn [e]
                           (.. e preventDefault)
                           (let [x (- (.-clientX e) 374)]
                             (set! (.-width style) (str x "px"))))
        handle-mouseup (fn [e] 
                         (.removeEventListener js/document "mousemove" handle-mousemove))]
    (.addEventListener js/document "mousemove" handle-mousemove)
    (.addEventListener js/document "mouseup" handle-mouseup (js-obj "once" true))
    (js/console.log element)
    (js/console.log event)))


; ;; Pagemarks
; FIXME: Element should not be selectable
(reg-event-fx 
  :pagemark
  (fn [{:keys [db]} _] 
    (let [page (-> (get db :pdf/viewer)
                   (.getPageView 0))
          text-layer (.. page -textLayer -textLayerDiv)
          parent (.createElement js/document "div")
          left   (.createElement js/document "div")
          right  (.createElement js/document "div")
          top    (.createElement js/document "div")
          bottom (.createElement js/document "div")
          ]
      (js/console.log (aget (.getClientRects (.-canvas page)) 0))

      (.setAttribute parent "style" "position: absolute; left 0; top: 0;
                                     height: 700px; width: 500px;
                                     max-width: 816px; min-width: 20px;
                                     pointer-events: none;")
      (.setAttribute left "style" "cursor: ew-resize; position: absolute; left: 0; top: 0;
                                   z-index: 9;
                                   width: 4px; height: 100%;
                                   pointer-events: auto;
                                   background-color: rgba(0,0,255,1);")
      (.setAttribute right "style" "cursor: ew-resize; position: absolute; right: 0; top: 0;
                                   z-index: 9;
                                   width: 4px; height: 100%;
                                   pointer-events: auto;
                                   background-color: rgba(0,0,255,1);")
      ; (.setAttribute right "onmousedown" pagemark-horizontal-resize)
      ; (js/console.log (.getAttribute right "onmousedown"))
      ; (.addEventListener left "mousedown" (fn [e] (if (= (obj/get e "button") 0)
      ;                                                (horizontal-resize parent e))))
      (.addEventListener right "mousedown" (fn [e] (if (= (.-button e) 0)
                                                     (horizontal-resize parent e))))


      (.setAttribute top "style" "cursor: ns-resize; position: absolute; left: 0; top: 0;
                                   width: 100%; height: 4px;
                                   background-color: rgba(0,0,255,1);")
      (.setAttribute bottom "style" "cursor: ns-resize; position: absolute; left: 0; bottom: 0;
                                     width: 100%; height: 4px;
                                     background-color: rgba(0,0,255,1);")
      (.append parent left)
      (.append parent right)
      (.append parent top)
      (.append parent bottom)
      (.append text-layer parent)





      )))