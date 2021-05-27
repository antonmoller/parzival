(ns parzival.pdf
  (:require
   [re-frame.core :as rf :refer [dispatch reg-event-fx reg-fx]]
   [parzival.db :as db]
   [parzival.style :refer [PAGEMARK-COLOR]]
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
      (.setAttribute "fill-opacity" opacity)
      (.addEventListener "click" #(dispatch [:highlight/toolbar-edit group])))
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

(def SVG-STYLE
  "position: absolute; inset: 0; width: 100%; height: 100%;
   mix-blend-mode: multiply; z-index: 1; pointer-events: none;")

(reg-event-fx
 :render/page-highlights
 (fn [{:keys [db]} [_ page]]
   (let [highlights (get-in db [:pdf/highlights (dec (.getAttribute page "data-page-number"))])
         page-rect  (.getBoundingClientRect (.querySelector page "canvas"))
         rows       (.-children (.querySelector page ".textLayer"))
         svg        (.createElementNS js/document SVG-NAMESPACE "svg")]
     (doto svg
       (.setAttribute "class" "highlightLayer")
       (.setAttribute "style" SVG-STYLE))
     (run! #(render-highlight (first %) (second %) svg page-rect rows) highlights)
     (.replaceChild (.querySelector page ".canvasWrapper")
                    svg
                    (.querySelector page ".highlightLayer"))
     {})))


(reg-event-fx
 :render/page
 (fn [{:keys [db]} [_ page]]
   (let [page-id (dec (.getAttribute page "data-page-number"))
         highlights (get-in db [:pdf/highlights page-id])
         pagemark (get-in db [:pdf/pagemarks page-id])
         pagemark-layer (.createElementNS js/document SVG-NAMESPACE "svg")
         highlight-layer (.createElementNS js/document SVG-NAMESPACE "svg")
         canvas-wrapper (.querySelector page ".canvasWrapper")]
     (doto highlight-layer
       (.setAttribute "class" "highlightLayer")
       (.setAttribute "style" SVG-STYLE))
     (doto pagemark-layer
       (.setAttribute "class" "pagemarkLayer")
       (.setAttribute "overflow" "visible")
       (.setAttribute "style" SVG-STYLE))
     {:fx [(.append canvas-wrapper highlight-layer)
           (.append canvas-wrapper pagemark-layer)
           (when (some? highlights)
             [:dispatch [:render/page-highlights page]])
           (when (some? pagemark)
             [:dispatch [:pagemark/render page pagemark]])]})))

(reg-event-fx
 :highlight/remove
 (fn [{:keys [db]} _]
   (let [{:keys [element _ _]} (get db :highlight/selected)
         page (.closest element ".page")]
     {:db (update-in db [:pdf/highlights (dec (.getAttribute page "data-page-number"))]
                     dissoc (.getAttribute element "id"))
      :fx [[:dispatch [:render/page-highlights page]]]})))

(reg-event-fx
 :highlight/edit
 (fn [{:keys [db]} [_ color opacity]]
   (let [{:keys [element _ _]} (get db :highlight/selected)
         page (.closest element ".page")]
     {:db (update-in db [:pdf/highlights
                         (dec (.getAttribute page "data-page-number"))
                         (.getAttribute element "id")]
                     assoc :color color :opacity opacity)
      :fx [[:dispatch [:render/page-highlights page]]]})))

(reg-event-fx
 :highlight/add
 (fn [{:keys [db]} [_ color opacity]]
   (let [selection (.getSelection js/document)]
     (when-not (.-isCollapsed selection)
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
         (.collapse range-obj)
         {:db (assoc-in db
                        [:pdf/highlights page-id]
                        (merge-highlights (get-in db [:pdf/highlights page-id])
                                          highlight))
          :fx [[:dispatch [:render/page-highlights page]]]})))))

(reg-event-fx
 :highlight/set-anchor
 (fn [{:keys [db]} [_ rect]]
   {:db (if (nil? rect)
          (assoc db :highlight/anchor nil)
          (as-> (.getBoundingClientRect (.getElementById js/document "viewer")) page-rect
            (assoc db
                   :highlight/anchor
                   {:left (+ (- (.-x rect) (.-x page-rect)) (/ (.-width rect) 2))
                    :top (- (.-bottom rect) (.-y page-rect))
                    :page-right (.-right page-rect)})))}))

(rf/reg-event-db
 :highlight/selected
 (fn [db [_ element]]
   (assoc db :highlight/selected element)))

(reg-event-fx
 :highlighting/close
 (fn [{:keys [db]} _]
   (as->  (:element (get db :highlight/selected)) selected-highlight
     (.addEventListener js/document "mousedown"
                        (fn []
                          (dispatch [:highlight/selected nil])
                          (dispatch [:highlight/set-anchor nil])
                          (when (some? selected-highlight)
                            (.setAttribute selected-highlight
                                           "fill-opacity"
                                           (* (.getAttribute selected-highlight "fill-opacity") 2))))
                        (js-obj "once" true)))))

(reg-event-fx
 :highlight/toolbar-edit
 (fn [_ [_ target]]
   {:fx [[:dispatch [:highlight/selected {:element target
                                          :color (.getAttribute target "fill")}]]
         [:dispatch [:highlight/set-anchor (.getBoundingClientRect target)]]
         (.setAttribute target "fill-opacity" (/ (.getAttribute target "fill-opacity") 2))]}))

(reg-event-fx
 :highlight/toolbar-create
 (fn [_ _]
   (when-let [selection-rect (as-> (.getSelection js/window) selection
                               (when-not (.-isCollapsed selection)
                                 (as-> (.getRangeAt selection 0) range-obj
                                   (when-not (= (.. range-obj -commonAncestorContainer -className) "pdfViewer")
                                     (.getBoundingClientRect range-obj)))))]
     {:fx [[:dispatch [:highlight/set-anchor selection-rect]]]})))

;;; Pagemarks

;; Helpers

(def stroke-width 8)

(defn px-to-percentage
  [bounding-px new-px]
  (-> new-px
      (/ bounding-px)
      (* 100)
      (str "%")))

(defn resize-handler
  [e]
  (let [target (.-target e)
        cursor (.getAttribute target "cursor")
        b-box ^js (.getBBox target)
        page-rect (.getBoundingClientRect (.closest target ".pagemarkLayer"))
        page-width-px (.-width page-rect)
        page-height-px (.-height page-rect)
        width-px (+ (.-width b-box) (.-movementX e))
        height-px (+ (.-height b-box) (.-movementY e))]
    (doto target
      (.setAttribute "width" (if (contains? #{"ew-resize" "nwse-resize"} cursor)
                               (cond 
                                 (< width-px 100) (px-to-percentage page-width-px 100)
                                 (< page-width-px width-px) "100%"
                                 :else (px-to-percentage page-width-px width-px))
                               (.getAttribute target "width")))
      (.setAttribute "height" (if (contains? #{"ns-resize" "nwse-resize"} cursor)
                                (cond
                                  (< height-px 100) (px-to-percentage page-height-px 100)
                                  (< page-height-px height-px) "100%"
                                  :else (px-to-percentage page-height-px height-px))
                                (.getAttribute target "height"))))))

(defn page-id
  [target]
  (-> (.closest target ".page")
      (.getAttribute "data-page-number")
      (dec)))

(defn set-cursor
  [target x y]
  (let [b-box ^js (.getBBox target)
        width (.-width b-box)
        height (.-height b-box)]
    (cond
      (and (<= (- width stroke-width) x width)
           (<= (- height stroke-width) y height))
      (.setAttribute target "cursor" "nwse-resize") ; lower right corner
      (and (<= (- width stroke-width) x width)
           (<= stroke-width y (- height stroke-width)))
      (.setAttribute target "cursor" "ew-resize") ; right
      (and (<= stroke-width x (- width stroke-width))
           (<= (- height stroke-width) y height))
      (.setAttribute target "cursor" "ns-resize") ; bottom
      :else (.setAttribute target "cursor" "default"))))

;; Subs

(rf/reg-sub
 :pagemark/anchor
 (fn [db _]
   (get db :pagemark/anchor)))

;; Events

(reg-event-fx
 :pagemark/resize
 (fn [{:keys [db]} [_ target]]
   {:db (update-in db 
                   [:pdf/pagemarks (page-id target)] 
                   assoc
                   :width (.getAttribute target "width") 
                   :height (.getAttribute target "height"))}))

(reg-event-fx
 :pagemark/render
 (fn [_ [_ page pagemark]]
   (let [svg (.querySelector page ".pagemarkLayer")
         rect (.createElementNS js/document SVG-NAMESPACE "rect")]
     (doto rect
       (.setAttribute "class" "pagemark")
       (.setAttribute "pointer-events" "auto")
       (.setAttribute "width" (:width pagemark))
       (.setAttribute "height" (:height pagemark))
       (.setAttribute "fill" "none")
       (.setAttribute "stroke-width" stroke-width)
       (.setAttribute "stroke" (:color pagemark))
       (.addEventListener "pointermove" (fn [e]
                                          (when-not (= (.-buttons e) 1)
                                            (set-cursor (.-target e) (.-offsetX e) (.-offsetY e)))))
       (.addEventListener "pointerdown" (fn [e]
                                          (when (= (.-buttons e) 1)
                                            (doto (.-target e)
                                              (.setAttribute "fill" (:done PAGEMARK-COLOR))
                                              (.setAttribute "fill-opacity" 0.2))
                                            (.addEventListener rect "pointermove" resize-handler)
                                            (.setPointerCapture rect (.-pointerId e)))))
       (.addEventListener "pointerup" (fn [e]
                                        (.removeEventListener rect "pointermove" resize-handler)
                                        (.releasePointerCapture rect (.-pointerId e))
                                        (doto (.-target e)
                                          (.setAttribute "fill" "none")
                                          (.setAttribute "cursor" "default"))
                                        (dispatch [:pagemark/resize (.-target e)]))))
     {:fx [(.append svg rect)]})))

(reg-event-fx
 :pagemark/add
 (fn [{:keys [db]} [_ page height]]
   (let [page-id (dec (.getAttribute page "data-page-number"))
         old-pagemark (.item (.getElementsByClassName page "pagemark") 0)
         pagemark {:color (:done PAGEMARK-COLOR)
                   :width "100%"
                   :height height}]
     (when (some? old-pagemark)
       (.remove old-pagemark))
     {:db (assoc-in db [:pdf/pagemarks page-id] pagemark)
      :fx [[:dispatch [:pagemark/render page pagemark]]]})))

(reg-event-fx
 :pagemark/remove
 (fn [{:keys [db]} [_ page]]
   (let [page-id (dec (.getAttribute page "data-page-number"))]
     (.remove (.item (.getElementsByClassName page "pagemark") 0))
     {:db (update db :pdf/pagemarks dissoc page-id)})))

(rf/reg-event-db
 :pagemark/set-anchor
 (fn [db [_ coords]]
   (assoc db :pagemark/anchor coords)))

(reg-event-fx
 :pagemark/close
 (fn [_ _]
   (.addEventListener js/document "mousedown"
                      #(dispatch [:pagemark/set-anchor nil])
                      (js-obj "once" true))))

(reg-event-fx
 :pagemark/menu
 (fn [_ [_ target x y]]
   (let [page (.closest target ".page")
         viewer-rect (.getBoundingClientRect (.getElementById js/document "viewer"))
         page-rect (.getBoundingClientRect page)
         viewer-height (-> (.getElementById js/document "viewerContainer")
                           (.getBoundingClientRect)
                           (.-height))
         style (.getComputedStyle js/window (.getElementById js/document "pagemark-menu"))
         menu-width (js/parseFloat (.-width style))
         menu-height (js/parseFloat (.-height style))
         left (if (< (+ x menu-width) (- (.-right viewer-rect) 20))
                (str (- x (.-x viewer-rect)) "px")
                (str (- (.-width viewer-rect) menu-width 20) "px"))
         top (if (< (+ y menu-height) (- viewer-height 10))
               (str (- y (.-y viewer-rect)) "px")
               (str (- y (.-y viewer-rect) menu-height 10) "px"))]
     {:fx [[:dispatch [:pagemark/set-anchor 
                       {:left left
                        :top top
                        :height (-> (- y (.-y page-rect))
                                    (/ (.-height page-rect))
                                    (* 100)
                                    (str "%"))
                        :edit (not= 0 (.-length (.getElementsByClassName page "pagemark")))
                        :page page}]]]})))
                                             