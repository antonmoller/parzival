(ns parzival.pdf
  (:require
   [re-frame.core :as rf :refer [dispatch reg-event-fx reg-cofx reg-fx  inject-cofx on-changes after]]
   [parzival.db :as db]
   ["pdfjs-dist" :as pdfjs]
   ["pdfjs-dist/web/pdf_viewer.js" :as pdfjs-viewer]
   [cljs.core.async :refer [go go-loop <! timeout]]
   [cljs.core.async.interop :refer [<p!]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def SVG-NAMESPACE "http://www.w3.org/2000/svg")

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

(rf/reg-event-db
 :highlight/toggle
 (fn [db _]
   (update db :highlight/open not)))

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

(defn get-end
  [range-obj]
  (let [end (.-endContainer range-obj)]
    (if (= (.-nodeName end) "SPAN")
      [(.-previousSibling end)
       (.. end -previousSibling -lastChild -length)]
      [(.-parentNode end)
       (.-endOffset range-obj)])))

(defn render-highlight
  [{:keys [color opacity start-id end-id start-offset end-offset]} svg page-rect rows]
  (let [fragment (js/DocumentFragment.)
        r (js/Range.)]
    (doseq [i (range start-id (inc end-id))]
      (.setStart r (text (.item rows i)) (if (== i start-id) start-offset 0))
      (.setEnd r (text (.item rows i)) (if (== i end-id) end-offset (length (.item rows i))))
      (let [coords (.getBoundingClientRect r)
            rect (.createElementNS js/document SVG-NAMESPACE "rect")]
        (.setAttribute rect "x" (- (.-left coords) (.-left page-rect)))
        (.setAttribute rect "y" (- (.-top coords) (.-top page-rect)))
        (.setAttribute rect "width" (.-width coords))
        (.setAttribute rect "height" (dec (.-height coords)))
        (.setAttribute rect "style" (str "cursor: pointer; pointer-events: auto;" 
                                         "fill: " color ";"
                                         "fill-opacity: " opacity ";")
        (.append fragment rect)))
    (.append svg fragment))))

(reg-event-fx
 :render/page
 (fn [{:keys [db]} [_ canvas-wrapper page-id]]
   (let [svg (.createElementNS js/document SVG-NAMESPACE "svg")]
     (.setAttribute svg "style" (str "position: absolute; inset: 0; width: 100%; height: 100%;
                                      mix-blend-mode: multiply; z-index: 1; pointer-events: none;"))
     (.append canvas-wrapper svg)
   )))
    ;; (let [page (.getPageView (get db :pdf/viewer) page-id)]
    ;;   (run! #(render-highlight (second %) page) (get-in db [:pdf/highlights page-id])))))

(reg-event-fx
 :highlight
 (fn [{:keys [db]} [_ color opacity]]
   (let [range-obj (get db :pdf/selection)
         text-layer (.. range-obj -startContainer -parentNode -parentNode)
         page (.-parentNode text-layer)
         page-id (dec (.getAttribute page "data-page-number"))
         page-rect (.getBoundingClientRect (.. page -firstChild -firstChild))
         svg       (.. page -firstChild -lastChild)
         rows       (.-children text-layer)
         rows-arr (.from js/Array rows)
         [end end-offset] (get-end range-obj)
         highlight {:color color
                    :opacity opacity
                    :start-id (.indexOf rows-arr (.. range-obj -startContainer -parentNode))
                    :end-id (.indexOf rows-arr end)
                    :start-offset (.-startOffset range-obj)
                    :end-offset end-offset}]
     (.collapse range-obj)
     {:db (if-let [page-highlights (get-in db [:pdf/highlights page-id])]
            (update-in db [:pdf/highlights page-id] assoc (count page-highlights) highlight)
            (assoc-in db [:pdf/highlights page-id 0] highlight))
      :fx [(render-highlight highlight svg page-rect rows)
           [:dispatch [:highlight/toggle]]]})))

(defn toolbar-anchor
  []
  (let [range-obj (.getRangeAt (.getSelection js/document) 0)]
    (when-not (.-collapsed range-obj)
      (let [text-layer-start (.. range-obj -startContainer -parentNode -parentNode)
            text-layer-end   (.. range-obj -endContainer -parentNode -parentNode)
            end-node         (.-endContainer range-obj)
            r                (js/Range.)]
        (when (.isSameNode text-layer-start text-layer-end)
          (.setStart r end-node 0)
          (.setEnd r end-node (.-length end-node))
          (as-> (.getBoundingClientRect r) rect
            [range-obj (+ (.-left rect) (/ (.-width rect) 2)) (+ (.-bottom rect) 5)]))))))

(reg-event-fx
 :highlight/toolbar
 (fn [{:keys [db]} _]
   (when-let [[selection x y] (toolbar-anchor)]
     {:db (-> db
              (assoc :highlight/anchor [x y])
              (assoc :pdf/selection selection))
      :fx [[:dispatch [:highlight/toggle]]]})))

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
          #(dispatch [:render/page (.. % -source -textLayerDiv -previousSibling) (dec (.-pageNumber %))]))
     )))


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
         bottom (.createElement js/document "div")]
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
     (.append text-layer parent))))