(ns parzival.pdf
  (:require
   [re-frame.core :as rf :refer [subscribe dispatch reg-event-fx reg-fx reg-event-db reg-sub after]]
   [parzival.style :refer [PAGEMARK-COLOR HIGHLIGHT-COLOR]]
   [parzival.utils :refer [gen-uid]]
   ["pdfjs-dist" :as pdfjs]
   ["pdfjs-dist/web/pdf_viewer.js" :as pdfjs-viewer]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def SVG-NAMESPACE "http://www.w3.org/2000/svg")
(def path (js/require "path"))
(def fs (js/require "fs"))

;;; Pdf

;;; Highlights

;;; Pagemarks

(reg-sub
 :pdf/num-pages
 :<- [:page/active]
 :<- [:pages]
 (fn [[page-uid pages]  _]
   (get-in pages [page-uid :num-pages])))

(reg-fx
 :pdf/document
 (fn [{:keys [data viewer worker]}]
   (go
     (try
       (let [pdf (<p! (.-promise (.getDocument pdfjs (js-obj "data" data "worker" worker))))]
         (.setDocument viewer pdf)
         (.setDocument (.-linkService ^js viewer) pdf nil))
       (catch js/Error e (js/console.log (ex-cause e)))))))

(reg-event-fx
 :pdf/load
 (fn [{:keys [db]} [_ uid pdf-filename]]
   (let [pdf-viewer (get db :pdf/viewer)
         pdf-worker (get db :pdf/worker)
         pdf-file (as-> (get db :db/filepath) p
                    (.dirname path p)
                    (.resolve path p "pdfs")
                    (.resolve path p pdf-filename)
                    (.readFileSync fs p))]
     {:fx [[:dispatch [:page/set-active uid]]
           [:pdf/document {:data pdf-file
                           :worker pdf-worker
                           :viewer pdf-viewer}]]})))

(reg-event-fx
 :pdf/init-viewer
 (fn [{:keys [db]} _]
   (let [container (.getElementById js/document "viewerContainer")
         viewer    (.getElementById js/document "viewer")
         event-bus (pdfjs-viewer/EventBus.)
         link-service (pdfjs-viewer/PDFLinkService. (js-obj "eventBus" event-bus "externalLinkTarget" 2))
        ;;  download-manager (pdfjs-viewer/DownloadManager.)
        ;;  find-controller (pdfjs-viewer/PDFFindController. (js-obj "eventBus" event-bus "linkService" link-service))
         pdf-viewer (pdfjs-viewer/PDFViewer. (js-obj "container" container
                                                     "viewer" viewer
                                                     "eventBus" event-bus
                                                     "linkService" link-service
                                                    ;;  "downloadManager" download-manager
                                                    ;;  "findController" find-controller
                                                     "textLayerMode" 2))]
     (set! (.. pdfjs -GlobalWorkerOptions -workerSrc) "./js/compiled/pdf.worker.js")
     (.setViewer link-service pdf-viewer)
     (js/console.log pdf-viewer)
     (.on event-bus "pagesinit" #(set! (.-currentScaleValue pdf-viewer) "page-width"))
     (.on event-bus "textlayerrendered" #(dispatch [:render/page (.. % -source -textLayerDiv -parentNode)]))
     {:db (-> db
              (assoc :pdf/viewer pdf-viewer)
              (assoc :pdf/worker (pdfjs/PDFWorker. "pdf-worker")))})))

(reg-event-fx
 :pdf/change-size
 (fn [{:keys [db]} _]
   (when-let [pdf-viewer (get db :pdf/viewer)]
     (set! (.-currentScaleValue pdf-viewer) "page-width"))
   {}))

;; TODO: Create a more general version by looking at pagemark-resize
(rf/reg-event-fx
 :pdf/resize
 (fn [_ [_ button target pointer-id id]]
   (when (= 1 button)
     (let [component-to-resize (.closest target (str "#" id))
           handle-resize (fn [e]
                           (set! (.. component-to-resize -style -width)
                                 (-> (js/parseInt (.. component-to-resize -style -width))
                                     (- (.-movementX e))
                                     (str "px"))))]
       (doto target
         (.setPointerCapture pointer-id)
         (.addEventListener "pointermove" handle-resize)
         (.addEventListener "pointerup" (fn [_]
                                          (.removeEventListener target "pointermove" handle-resize)
                                          (.releasePointerCapture target pointer-id)
                                          (dispatch [:pdf/change-size]))
                            (js-obj "once" true)))))
   {}))

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

(defn is-overlapping?
  [{x0-0 :start-offset y0-0 :start x0-1 :end-offset y0-1 :end}
   {x1-0 :start-offset y1-0 :start x1-1 :end-offset y1-1 :end}]
  (or
   (and (< y0-0 y1-1) (> y0-1 y1-0))
   (and (== y0-0 y0-1 y1-0 y1-1) (and (< x0-0 x1-1) (> x0-1 x1-0)))
   (and (== y0-1 y1-0) (not= y0-0 y0-1 y1-0 y1-1) (> x0-1 x1-0))
   (and (== y0-0 y1-1) (not= y0-0 y0-1 y1-0 y1-1) (< x0-0 x1-1))))

(defn merge-
  [{c :color x0-0 :start-offset y0-0 :start x0-1 :end-offset y0-1 :end}
   {x1-0 :start-offset y1-0 :start x1-1 :end-offset y1-1 :end}]
  {:color c
   :start (min y0-0 y1-0)
   :start-offset (cond
                   (< y0-0 y1-0) x0-0
                   (== y0-0 y1-0) (min x0-0 x1-0)
                   :else x1-0)
   :end (max y0-1 y1-1)
   :end-offset   (cond
                   (> y0-1 y1-1) x0-1
                   (== y0-1 y1-1) (max x0-1 x1-1)
                   :else x1-1)})

(defn dec-to-percentage
  [dec]
  (-> dec (* 100) (str "%")))

(rf/reg-sub
 :highlight/anchor
 (fn [db _]
   (get db :highlight/anchor)))

(rf/reg-sub
 :highlight/edit
 (fn [db _]
   (get db :highlight/selected)))

(reg-event-fx
 :highlight/render
 (fn [_ [_ {:keys [color start start-offset end end-offset]} highlight-uid svg page-rect rows]]
   (let [group (.createElementNS js/document SVG-NAMESPACE "g")
         r (js/Range.)]
     (doto group
       (.setAttribute "id" highlight-uid)
       (.addEventListener "click" #(dispatch [:highlight/toolbar-edit group]))
       (.setAttribute "style" (str "cursor: pointer; pointer-events: auto; "
                                    "fill: " (:color (color HIGHLIGHT-COLOR))
                                    "; fill-opacity: " (:opacity (color HIGHLIGHT-COLOR)) ";")))
     (doseq [i (range start (inc end))]
       (.setStart r (.-firstChild (.item rows i)) (if (== i start) start-offset 0))
       (.setEnd r (.-firstChild (.item rows i)) (if (== i end) end-offset (.. (.item rows i) -firstChild -length)))
       (let [coords (.getBoundingClientRect r)
             rect (.createElementNS js/document SVG-NAMESPACE "rect")]
         (.setAttribute rect "style" (str "x: " (- (.-x coords) (.-x page-rect)) "px;"
                                          "y: " (- (.-y coords) (.-y page-rect)) "px;"
                                          "width: " (.-width coords) "px;"
                                          "height: " (.-height coords) "px;"))
         (.append group rect)))
     (.append svg group))))

(reg-event-fx
 :page/render-highlights
 (fn [{:keys [db]} [_ page-uid page-no page svg]]
   (when-let [highlights (get-in db [:pages page-uid :highlights page-no])]
     (let [page-rect  (.getBoundingClientRect (.querySelector page "canvas"))
           containers (.-children (.querySelector page ".textLayer"))]
       {:fx (into []
                  (map
                   #(vector :dispatch [:highlight/render (second %) (first %) svg page-rect containers])
                   highlights))}))))

(reg-event-fx
 :render/page
 (fn [{:keys [db]} [_ page]]
   (let [page-uid (get db :page/active)
         page-no (int (.getAttribute page "data-page-number"))
         svg (.createElementNS js/document SVG-NAMESPACE "svg")
         canvas-wrapper (.querySelector page ".canvasWrapper")
         pagemark (get-in db [:pages page-uid :pagemarks page-no])]
     (js/console.log pagemark)
     (doto svg
       (.setAttribute "class" "svgLayer")
       (.setAttribute "style" "position: absolute; inset: 0; width: 100%; height: 100%;
                               mix-blend-mode: multiply; z-index: 1; pointer-events: none; 
                               overflow: visible;"))
     {:fx [(.append canvas-wrapper svg)
           [:dispatch [:page/render-highlights page-uid page-no page svg]]
           (when (some? pagemark)
             [:dispatch [:pagemark/render pagemark svg]])]})))

(reg-event-fx
 :highlight/remove
 (fn [{:keys [db]} _]
   (let [{:keys [element _ _]} (get db :highlight/selected)
         page-uid (get db :page/active)
         page (.closest element ".page")]
     {:db (update-in db [:pages page-uid :highlights (int (.getAttribute page "data-page-number"))]
                     dissoc (.getAttribute element "id"))
      :fx [(.remove element)]})))


(reg-event-fx
 :highlight/edit
 (fn [{:keys [db]} [_ color]]
   (let [{:keys [element _ _]} (get db :highlight/selected)
         page-uid (get db :page/active)
         page (.closest element ".page")]
     (set! (.. element -style -fill) (:color (color HIGHLIGHT-COLOR)))
     (set! (.. element -style -fillOpacity) (:opacity (color HIGHLIGHT-COLOR)))
     {:db (update-in db [:pages page-uid :highlights
                         (int (.getAttribute page "data-page-number"))
                         (.getAttribute element "id")]
                     assoc :color color)})))

(defn merge-highlights
  [page highlight]
  (reduce-kv (fn [m k highlight]
               (if (is-overlapping? (:merged m) highlight)
                 (do
                   (.remove (.getElementById js/document k))
                   (assoc m :merged (merge- (:merged m) highlight)))
                 (assoc-in m [:highlights k] highlight)))
             {:merged highlight :highlights {}}
             page))

(reg-event-fx
 :highlight/add
 (fn [{:keys [db]} [_ color]]
   (let [selection (.getSelection js/document)]
     (when-not (.-isCollapsed selection)
       (let [page-uid (get db :page/active)
             range-obj (.getRangeAt selection 0)
             start-container (.. range-obj -startContainer -parentNode)
             page      (.closest start-container ".page")
             page-id   (int (.getAttribute page "data-page-number"))
             page-rect (.getBoundingClientRect (.querySelector page "canvas"))
             svg (.querySelector page ".svgLayer")
             containers (.-children (.closest start-container ".textLayer"))
             container-arr (.from js/Array containers)
             [end-container end-offset]  (get-end range-obj)
             {:keys [merged highlights]} (merge-highlights (get-in db [:pages page-uid :highlights page-id])
                                                           {:color color
                                                            :start (.indexOf container-arr start-container)
                                                            :start-offset (.-startOffset range-obj)
                                                            :end (.indexOf container-arr end-container)
                                                            :end-offset end-offset})
             merged-uid (gen-uid "highlight")]
         (.collapse range-obj)
         {:db (->> (assoc highlights merged-uid merged)
                   (assoc-in db [:pages page-uid :highlights page-id]))
          :fx [[:dispatch [:highlight/render merged merged-uid svg page-rect containers]]]})))))

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
                            (set! (.. selected-highlight -style -fillOpacity)
                                  (* (.. selected-highlight -style -fillOpacity) 2))))
                        (js-obj "once" true)))))

(reg-event-fx
 :highlight/toolbar-edit
 (fn [_ [_ target]]
   (set!  (.. target -style -fillOpacity) (/ (.. target -style -fillOpacity) 2))
   {:fx [[:dispatch [:highlight/selected {:element target
                                          :color (.. target -style -fill)}]]
         [:dispatch [:highlight/set-anchor (.getBoundingClientRect target)]]]}))

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
(def min-px 100)

(defn px-to-percentage
  [bounding-px new-px]
  (-> new-px
      (/ bounding-px)
      (* 100)
      (str "%")))

(defn percentage-to-float
  [p]
  (-> (js/parseFloat p)
      (/ 100)))

(defn resize-handler
  [e]
  (let [target (.-target e)
        style (.-style target)
        cursor (.. target -style -cursor)
        b-box ^js (.getBBox target)
        page-rect (.getBoundingClientRect (.closest target ".svgLayer"))
        page-width-px (.-width page-rect)
        page-height-px (.-height page-rect)
        width-px (+ (.-width b-box) (.-movementX e))
        height-px (+ (.-height b-box) (.-movementY e))]
    (set! (.-width style) (if (contains? #{"ew-resize" "nwse-resize"} cursor)
                            (cond
                              (< width-px min-px) (px-to-percentage page-width-px min-px)
                              (> width-px page-width-px) "100%"
                              :else (px-to-percentage page-width-px width-px))
                            (.-width style)))
    (set! (.-height style) (if (contains? #{"ns-resize" "nwse-resize"} cursor)
                             (cond
                               (< height-px min-px) (px-to-percentage page-height-px min-px)
                               (> height-px page-height-px) "100%"
                               :else (px-to-percentage page-height-px height-px))
                             (.-height style)))))

(defn page-id
  [target]
  (-> (.closest target ".page")
      (.getAttribute "data-page-number")
      (int)))

(defn set-cursor
  [target x y]
  (let [b-box ^js (.getBBox target)
        width (.-width b-box)
        height (.-height b-box)]
    (cond
      (and (<= (- width stroke-width) x width)
           (<= (- height stroke-width) y height))
      (set! (.. target -style -cursor) "nwse-resize") ; lower right corner
      (and (<= (- width stroke-width) x width)
           (<= stroke-width y (- height stroke-width)))
      (set! (.. target -style -cursor) "ew-resize") ; right
      (and (<= stroke-width x (- width stroke-width))
           (<= (- height stroke-width) y height))
      (set! (.. target -style -cursor) "ns-resize") ; bottom
      :else (set! (.. target -style -cursor) "default"))))

;; Subs

(rf/reg-sub
 :pagemark/anchor
 (fn [db _]
   (get db :pagemark/anchor)))

(rf/reg-sub
 :pagemark?
 (fn [db _]
   (get db :pagemark?)))

(rf/reg-sub
 :pagemark/sidebar
 (fn [db _]
   (get db :pagemark/sidebar)))

(rf/reg-sub
 :pdf/pagemarks
 (fn [db _]
   (get db :pdf/pagemarks)))

(defn merge?
  [p-0 p-1]
  (and
   (some? p-0)
   (= (inc (:end-page p-0)) (:start-page p-1))
   (= 1 (:end-area p-0))
   (= (:type p-0) (:type p-1))
   (and (some? (:schedule p-0)) (= (:schedule p-0) (:schedule p-1)))))

(defn merge-pagemarks
  [{:keys [type schedule start-page]} p-1]
  {:type type
   :schedule schedule
   :start-page start-page
   :end-page (:end-page p-1)
   :end-area (:end-area p-1)})

(defn get-type
  [{:keys [done skip schedule]}]
  (cond
    (some? done) :done
    skip :skip
    (not= "" schedule) :schedule))

(defn group-consecutive-pages
  [pagemarks]
  (reduce-kv (fn [[head & tail] k v]
               (let [pagemark {:type (get-type v)
                               :schedule (:schedule v)
                               :start-page (inc k)
                               :end-page (inc k)
                               :end-area (if (some? (:done v))
                                           (-> (js/parseFloat (get-in v [:done :width]))
                                               (* (js/parseFloat (get-in v [:done :height])))
                                               (/ 10000))
                                           1)}]
                 (cond
                   (merge? head pagemark) (conj tail (merge-pagemarks head pagemark))
                   (some? head) (conj tail head pagemark)
                   :else (conj tail pagemark))))
             '()
             (into (sorted-map) pagemarks)))

(rf/reg-sub
 :pdf/pagemarks-sidebar
 :<- [:page/active]
 :<- [:pages]
 (fn [[page-uid pages] _]
   (js/console.log
     (->> (get-in pages [page-uid :pagemarks])
          (map (fn [[_ v]] v))
          (doto)))
   (-> (get-in pages [page-uid :pagemarks])
       (group-consecutive-pages))))

;; Events

(rf/reg-event-db
 :pagemark-state
 (fn [db [_ bool]]
   (assoc db :pagemark? bool)))

(reg-event-fx
 :pagemark?
 (fn [_ _]
   (.addEventListener js/document
                      "pointerdown"
                      (fn close-pagemark [e]
                        (when (nil? (.closest (.-target e) "#createPagemark"))
                          (.removeEventListener js/document "pointerdown" close-pagemark)
                          (dispatch [:pagemark-state false]))))
   {:fx [[:dispatch [:pagemark-state true]]]}))

;; TODO: Will always be :done and hence have :width and :height
(rf/reg-event-db
 :pagemark/resize
 (fn [db [_ target]]
   (assoc-in db
             [:pages (get db :page/active) :pagemarks (page-id target)]
             {:width (percentage-to-float (.. target -style -width))
              :height (percentage-to-float (.. target -style -height))})))

(defn pagemark-done
  [rect width height]
  (doto rect
    (.setAttribute "class" "pagemark")
    (-> (.-style)
        (set! (str "width: " width "; height:" height
                   "; fill: none; stroke-width:" stroke-width "; stroke:"
                   (:done PAGEMARK-COLOR) "; pointer-events: auto;")))
    (.addEventListener "pointermove" (fn [e]
                                       (when-not (= (.-buttons e) 1)
                                         (set-cursor (.-target e) (.-offsetX e) (.-offsetY e)))))
    (.addEventListener "pointerdown" (fn [e]
                                       (when (= (.-buttons e) 1)
                                         (set! (.. e -target -style -fill) (:done PAGEMARK-COLOR))
                                         (set! (.. e -target -style -fillOpacity) 0.2)
                                         (.addEventListener rect "pointermove" resize-handler)
                                         (.setPointerCapture rect (.-pointerId e)))))
    (.addEventListener "pointerup" (fn [e]
                                     (.removeEventListener rect "pointermove" resize-handler)
                                     (.releasePointerCapture rect (.-pointerId e))
                                     (set! (.. e -target -style -fill) "none")
                                     (set! (.. e -target -style -cursor) "default")
                                     (dispatch [:pagemark/resize (.-target e)])))))

(defn pagemark-skip
  [rect]
  (doto rect
    (.setAttribute "class" "pagemark")
    (.setAttribute "style" (str "pointer-events: auto; width: 100%; height: 100%;
                                 fill: " (:skip PAGEMARK-COLOR) "; fill-opacity: 0.3;"))))

(reg-event-fx
 :pagemark/render
 (fn [_ [_ pagemark svg]]
   (let [rect (.createElementNS js/document SVG-NAMESPACE "rect")]
     (cond
       (and (contains? pagemark :width)
            (contains? pagemark :height)) (pagemark-done rect
                                                         (dec-to-percentage (:width pagemark))
                                                         (dec-to-percentage (:height pagemark)))
       (contains? pagemark :skip?) (pagemark-skip rect))
     {:fx [(.append svg rect)]})))

(defn group-pages
  [pages]
  (reduce (fn [[head & tail] v]
            (if (or (nil? (first head)) (= v (inc (first head))))
              (conj tail (conj head v))
              (conj tail head (list v))))
          '()
          (sort pages)))

(reg-event-fx
 :pagemark/add
 (fn [{:keys [db]} [_   page width height]]
   (let [page-no (int (.getAttribute page "data-page-number"))
         page-uid (get db :page/active)
         pagemark {:width width :height height}
         svg (.querySelector page ".svgLayer")]
     {:db (assoc-in db [:pages page-uid :pagemarks page-no] pagemark)
      :fx [[:dispatch [:pagemark/remove-render page]]
           [:dispatch [:pagemark/render pagemark svg]]]})))

(reg-event-fx
 :pagemark/remove-render
 (fn [_ [_ page]]
   (as-> (.querySelector page "rect.pagemark") pagemark
     {:fx [(when (some? pagemark)
             (.remove pagemark))]})))

(reg-event-fx
 :pagemark/remove
 (fn [{:keys [db]} [_ page]]
 (let [page-uid (get db :page/active)
       page-no (int (.getAttribute page "data-page-number"))]
   {:db (update-in db [:pages page-uid :pagemarks] dissoc page-no)
    :fx [[:dispatch [:pagemark/remove-render page]]]})))

(rf/reg-event-db
 :pagemark/set-anchor
 (fn [db [_ coords]]
   (assoc db :pagemark/anchor coords)))

(defn get-pages
  [start-page end-page]
  (range (dec (int start-page)) (int end-page)))

(defn add-pages
  [list start-page end-page]
  (concat list (range (dec (int start-page)) (int end-page))))

(defn page-rendered
  [page-no]
  (.querySelector js/document (str "div.page[data-page-number=\"" (inc page-no)
                                   "\"][data-loaded=\"true\"]")))

(defn create-pagemark-fx
  [pages fx]
  (reduce (fn [m v]
            (if-let [page (page-rendered v)]
              (conj m [:dispatch [fx page :skip]])
              m))
          []
          pages))

(rf/reg-event-fx
 :pagemark/sidebar-remove
 (fn [{:keys [db]} [_ start-page end-page deadline]]
   (let [pages (get-pages start-page end-page)]
     {:db (update db :pdf/pagemarks #(apply dissoc % pages))
      :fx (if (= "" deadline)
            (into [] (create-pagemark-fx pages :pagemark/remove-render))
            [])})))

(rf/reg-event-fx
 :pagemark/sidebar-add-edit
 (fn [{:keys [db]} [_ {:keys [start-page end-page deadline edit-start edit-end]}]]
   (let [pages-to-add (cond-> '()
                        (< (int start-page) (int edit-start)) (add-pages start-page (dec (int edit-start)))
                        true (add-pages edit-start edit-end)
                        (< (int edit-end) (int end-page)) (add-pages (inc (int edit-end)) end-page))
         pages-to-remove (cond-> '()
                           (< (int edit-start) (int start-page)) (add-pages edit-start
                                                                            (dec (int start-page)))
                           (< (int end-page) (int edit-end)) (add-pages (inc (int end-page))
                                                                        edit-end))]
     {:db (-> db
              (update :pdf/pagemarks into (zipmap pages-to-add
                                                  (repeat (count pages-to-add)
                                                          {:done nil ; Can't set :done from the scrollbar
                                                           :skip (= "" deadline)
                                                           :schedule deadline})))
              (update :pdf/pagemarks #(apply dissoc % pages-to-remove)))
      :fx (if (= "" deadline)
            (into [] (concat (create-pagemark-fx pages-to-add :pagemark/render)
                             (create-pagemark-fx pages-to-remove :pagemark/remove-render)))
            [])})))

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
               (str (- y (.-y viewer-rect) menu-height 10) "px"))
         height-px (- y (.-y page-rect))
         height (-> (if (> height-px min-px)
                      (/ height-px (.-height page-rect))
                      (/ min-px (.-height page-rect))))]
     {:fx [[:dispatch [:pagemark/set-anchor
                       {:left left
                        :top top
                        :height height
                        :edit? (not= 0 (.-length (.getElementsByClassName page "pagemark")))
                        :page page}]]]})))