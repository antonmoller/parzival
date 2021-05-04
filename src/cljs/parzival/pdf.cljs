(ns parzival.pdf
  (:require
   [re-frame.core :as rf :refer [dispatch reg-event-fx reg-fx]]
   [parzival.db :as db]
   ["pdfjs-dist" :as pdfjs]
   ["pdfjs-dist/web/pdf_viewer.js" :as pdfjs-viewer]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def SVG-NAMESPACE "http://www.w3.org/2000/svg")

;;; Subs

(rf/reg-sub
 :pdf?
 (fn [db _]
   (some? (get db :pdf))))


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
     (.on event-bus "textlayerrendered" #(dispatch [:render/page (.. % -source -textLayerDiv -parentNode)])))))

;;; Highlights

;; Helpers

(defn get-end
  [range-obj]
  (let [end (.-endContainer range-obj)]
    (if (= (.-nodeName end) "SPAN")
      [(.-previousSibling end)
       (.. end -previousSibling -lastChild -length)]
      [(.-parentNode end)
       (.-endOffset range-obj)])))

(defn highlight-id
  [{:keys [_ _ x0 y0 x1 y1]}]
  (str "highlight-" x0 "-" y0 "-" x1 "-" y1))

(defn is-overlapping?
  [{x0-0 :x0 y0-0 :y0 x0-1 :x1 y0-1 :y1} {x1-0 :x0 y1-0 :y0 x1-1 :x1 y1-1 :y1}]
  (or
   (and (< y0-0 y1-1) (> y0-1 y1-0))
   (and (== y0-0 y0-1 y1-0 y1-1) (and (< x0-0 x1-1) (> x0-1 x1-0)))
   (and (== y0-1 y1-0) (not= y0-0 y0-1 y1-0 y1-1) (> x0-1 x1-0))
   (and (== y0-0 y1-1) (not= y0-0 y0-1 y1-0 y1-1) (< x0-0 x1-1))))

(defn merge-
  [{c :color o :opacity x0-0 :x0 y0-0 :y0 x0-1 :x1  y0-1 :y1} {x1-0 :x0 y1-0 :y0 x1-1 :x1  y1-1 :y1}]
  {:color c
   :opacity o
   :x0 (cond
         (< y0-0 y1-0) x0-0
         (== y0-0 y1-0) (min x0-0 x1-0)
         :else x1-0)
   :x1   (cond
           (> y0-1 y1-1) x0-1
           (== y0-1 y1-1) (max x0-1 x1-1)
           :else x1-1)
   :y0 (min y0-0 y1-0)
   :y1   (max y0-1 y1-1)})

(defn merge-highlights
  [page highlight]
  (let [{:keys [merged highlights]} (reduce-kv (fn [m k highlight]
                                                 (if (is-overlapping? (:merged m) highlight)
                                                   (assoc m :merged (merge- (:merged m) highlight))
                                                   (assoc-in m [:highlights k] highlight)))
                                               {:merged highlight :highlights {}}
                                               page)]
    (assoc highlights (highlight-id merged) merged)))

(defn render-highlight
  [id {:keys [color opacity x0 x1 y0 y1]} svg page-rect rows]
  (let [group (.createElementNS js/document SVG-NAMESPACE "g")
        r (js/Range.)]
    (doto group
      (.setAttribute "id" id)
      (.setAttribute "cursor" "pointer")
      (.setAttribute "pointer-events" "auto")
      (.setAttribute "fill" color)
      (.setAttribute "fill-opacity" opacity))
    (doseq [i (range y0 (inc y1))]
      (.setStart r (.-firstChild (.item rows i)) (if (== i y0) x0 0))
      (.setEnd r (.-firstChild (.item rows i)) (if (== i y1) x1 (.. (.item rows i) -firstChild -length)))
      (let [coords (.getBoundingClientRect r)
            rect (.createElementNS js/document SVG-NAMESPACE "rect")]
        (doto rect
          (.setAttribute "x" (- (.-x coords) (.-x page-rect)))
          (.setAttribute "y" (- (.-y coords) (.-y page-rect)))
          (.setAttribute "width" (.-width coords))
          (.setAttribute "height" (.-height coords)))
        (.append group rect)))
    (.append svg group)))

;; Subs

(rf/reg-sub
 :highlight/anchor
 (fn [db _]
   (get db :highlight/anchor)))

(rf/reg-sub
 :highlight/edit
 (fn [db _]
   (get db :highlight/selected)))

;; Events

(reg-event-fx
 :render/page
 (fn [{:keys [db]} [_ page]]
   (let [highlights (get-in db [:pdf/highlights (dec (.getAttribute page "data-page-number"))])
         page-rect (.getBoundingClientRect (.. page -firstChild -firstChild))
         rows      (.. page -firstChild -nextSibling -children)
         svg (.createElementNS js/document SVG-NAMESPACE "svg")]
     (.setAttribute svg "class" "highlightLayer")
     (.setAttribute svg "style" (str "position: absolute; inset: 0; width: 100%; height: 100%;
                                      mix-blend-mode: multiply; z-index: 1; pointer-events: none;"))
     (run! #(render-highlight (first %) (second %) svg page-rect rows) highlights)
     (when (= (.. page -firstChild -lastChild -nodeName) "svg")
       (.removeChild (.-firstChild page) (.. page -firstChild -lastChild)))
     (.append (.-firstChild page) svg))))

(reg-event-fx
 :highlight/remove
 (fn [{:keys [db]} _]
   (let [{:keys [element _ _]} (get db :highlight/selected)
         page (.closest element ".page")]
     {:db (update-in db [:pdf/highlights (dec (.getAttribute page "data-page-number"))]
                     dissoc (.getAttribute element "id"))
      :fx [[:dispatch [:render/page page]]
           [:dispatch [:highlight/toolbar-close]]]})))

(reg-event-fx
 :highlight/edit
 (fn [{:keys [db]} [_ color opacity]]
   (let [{:keys [element _ _]} (get db :highlight/selected)
         page (.closest element ".page")]
     {:db (update-in db [:pdf/highlights
                         (dec (.getAttribute page "data-page-number"))
                         (.getAttribute element "id")]
                     assoc :color color :opacity opacity)
      :fx [[:dispatch [:render/page page]]
           [:dispatch [:highlight/toolbar-close]]]})))

(reg-event-fx
 :highlight/add
 (fn [{:keys [db]} [_ color opacity]]
   (let [selection (.getSelection js/document)]
     (if-not (.-isCollapsed selection)
       (let [range-obj        (.getRangeAt selection 0)
             text-layer       (.. range-obj -startContainer -parentNode -parentNode)
             page             (.-parentNode text-layer)
             page-id          (dec (.getAttribute page "data-page-number"))
             rows             (.-children text-layer)
             rows-arr         (.from js/Array rows)
             [end x1] (get-end range-obj)
             highlight        {:color color
                               :opacity opacity
                               :x0 (.-startOffset range-obj)
                               :y0 (.indexOf rows-arr (.. range-obj -startContainer -parentNode))
                               :x1 x1
                               :y1 (.indexOf rows-arr end)}]
         {:db (assoc-in db
                        [:pdf/highlights page-id]
                        (merge-highlights (get-in db [:pdf/highlights page-id])
                                          highlight))
          :fx [[:dispatch [:render/page page]]
               (.collapse range-obj)
               [:dispatch [:highlight/toolbar-close]]]})
       {:fx [[:dispatch [:highlight/toolbar-close]]]}))))

(reg-event-fx
 :highlight/set-anchor
 (fn [{:keys [db]} [_ rect]]
   {:db (if (nil? rect)
          (assoc db :highlight/anchor nil)
          (as-> (.getBoundingClientRect (.getElementById js/document "viewer")) page-rect
            (assoc db :highlight/anchor
                   {:page-left (.-left page-rect)
                    :page-right (.-right page-rect)
                    :anchor-x (+ (- (.-x rect) (.-x page-rect)) (/ (.-width rect) 2))
                    :anchor-y (+ (- (.-bottom rect) (.-y page-rect)) 5)})))}))

(rf/reg-event-db
 :highlight/selected
 (fn [db [_ element]]
   (assoc db :highlight/selected element)))

(reg-event-fx
 :highlight/toolbar-close
 (fn [_ _]
   {:fx [[:dispatch [:highlight/set-anchor nil]]
         [:dispatch [:highlight/selected nil]]]}))

(reg-event-fx
 :highlight/toolbar-create
 (fn [_ [_ selection-rect]]
   {:fx [[:dispatch [:highlight/set-anchor selection-rect]]]}))

(reg-event-fx
 :highlight/toolbar-edit
 (fn [_ [_ target]]
   {:fx [(.setAttribute target "fill-opacity" (/ (.getAttribute target "fill-opacity") 2))
         [:dispatch [:highlight/selected {:element target
                                          :color (.getAttribute target "fill")
                                          :opacity (.getAttribute target "fill-opacity")}]]
         [:dispatch [:highlight/set-anchor (.getBoundingClientRect target)]]]}))

(reg-event-fx
 :highlight/toolbar
 (fn [{:keys [db]} [_ target]]
   (let [selection-rect (as-> (.getSelection js/window) selection
                          (when-not (.-isCollapsed selection)
                            (as-> (.getRangeAt selection 0) range-obj
                              (when-not (= (.. range-obj -commonAncestorContainer -className) "pdfViewer")
                                (.getBoundingClientRect range-obj)))))
         {:keys [element _ _]} (get db :highlight/selected)]
     (when (and (some? element) (not (.isSameNode element target)))
       (.setAttribute element "fill-opacity" (* (.getAttribute element "fill-opacity") 2)))
     (cond
       (and (= (.-nodeName target) "g") (not (.isSameNode target element))) {:fx [[:dispatch [:highlight/toolbar-edit target]]]}
       (and (not= target "g") (some? selection-rect)) {:fx [[:dispatch [:highlight/selected nil]]
                                                                            [:dispatch [:highlight/toolbar-create selection-rect]]]}
       (not= target "g") {:fx [[:dispatch [:highlight/toolbar-close]]]}))))

;;; Pagemarks

;; Helpers

(defn set-cursor
  [target x-in y-in]
  (let [width (.getAttribute target "width")
        height (.getAttribute target "height")
        x (- x-in (js/parseInt (.getAttribute target "x")))
        y (- y-in (js/parseInt (.getAttribute target "y")))]
    (cond
      (and (<= 0 x 5) (<= 0 y 5)) (.setAttribute target "cursor" "se-resize") ; upper left corner
      (and (<= 0 x 5) (<= (- height 5) y height)) (.setAttribute target "cursor" "ne-resize") ; lower left corner
      (and (<= (- width 5) x width) (<= 0 y 5)) (.setAttribute target "cursor" "sw-resize") ; upper right corner
      (and (<= (- width 5) x width) (<= (- height 5) y height)) (.setAttribute target "cursor" "nw-resize") ; lower right corner
      (<= 0 x 5 y (- height 5)) (.setAttribute target "cursor" "w-resize") ; left
      (and (<= (- width 5) x width) (<= 5 y (- height 5))) (.setAttribute target "cursor" "e-resize") ; right
      (and (<= 5 x (- width 5)) (<= (- height 5) y height)) (.setAttribute target "cursor" "s-resize") ; bottom
      (<= 0 y 5 x (- width 5)) (.setAttribute target "cursor" "n-resize"); top
      )
    ))

(reg-event-fx
 :pagemark
 (fn [{:keys [db]} [_ target]]
   (let [page (.closest target ".page")
         svg (.querySelector page ".highlightLayer")
         rect (.createElementNS js/document SVG-NAMESPACE "rect")
         resize-handler (fn [e]
                          (case (.getAttribute (.-target e) "cursor")
                            "s-resize" (.setAttribute rect "height" (+ (js/parseInt (.getAttribute rect "height")) (.-movementY e)))
                            "e-resize" (.setAttribute rect "width" (+ (js/parseInt (.getAttribute rect "width")) (.-movementX e)))
                            "n-resize" (do
                                         (.setAttribute rect "y" (+ (js/parseInt (.getAttribute rect "y")) (.-movementY e)))
                                         (.setAttribute rect "height" (- (js/parseInt (.getAttribute rect "height")) (.-movementY e))))
                            "w-resize" (do
                                         (.setAttribute rect "x" (+ (js/parseInt (.getAttribute rect "x")) (.-movementX e)))
                                         (.setAttribute rect "width" (- (js/parseInt (.getAttribute rect "width")) (.-movementX e))))
                            "se-resize" (do
                                          (.setAttribute rect "x" (+ (js/parseInt (.getAttribute rect "x")) (.-movementX e)))
                                          (.setAttribute rect "y" (+ (js/parseInt (.getAttribute rect "y")) (.-movementY e)))
                                          (.setAttribute rect "width" (- (js/parseInt (.getAttribute rect "width")) (.-movementX e)))
                                          (.setAttribute rect "height" (- (js/parseInt (.getAttribute rect "height")) (.-movementY e))))
                            "ne-resize" (do
                                          (.setAttribute rect "x" (+ (js/parseInt (.getAttribute rect "x")) (.-movementX e)))
                                          (.setAttribute rect "width" (- (js/parseInt (.getAttribute rect "width")) (.-movementX e)))
                                          (.setAttribute rect "height" (+ (js/parseInt (.getAttribute rect "height")) (.-movementY e))))
                            "sw-resize" (do
                                          (.setAttribute rect "y" (+ (js/parseInt (.getAttribute rect "y")) (.-movementY e)))
                                          (.setAttribute rect "width" (+ (js/parseInt (.getAttribute rect "width")) (.-movementX e)))
                                          (.setAttribute rect "height" (- (js/parseInt (.getAttribute rect "height")) (.-movementY e))))
                            "nw-resize" (do
                                          (.setAttribute rect "width" (+ (js/parseInt (.getAttribute rect "width")) (.-movementX e)))
                                          (.setAttribute rect "height" (+ (js/parseInt (.getAttribute rect "height")) (.-movementY e))))))]
     (doto rect
       (.setAttribute "x" 0)
       (.setAttribute "y" 0)
       (.setAttribute "width" (js/parseInt (.. page -style -width)))
       (.setAttribute "height" 700)
       (.setAttribute "stroke-width" 5)
       (.setAttribute "pointer-events" "auto")
       (.setAttribute "fill" "none")
       (.setAttribute "stroke" "blue")
       (.addEventListener "pointermove" (fn [e]
                                          (when-not (= (.-buttons e) 1)
                                            (set-cursor (.-target e) (.-offsetX e) (.-offsetY e)))))
       (.addEventListener "pointerdown" (fn [e]
                                          (.addEventListener rect "pointermove" resize-handler)
                                          (.setPointerCapture rect (.-pointerId e))))
       (.addEventListener "pointerup" (fn [e]
                                        (.removeEventListener rect "pointermove" resize-handler)
                                        (.releasePointerCapture rect (.-pointerId e)))))
     (.append svg rect))))