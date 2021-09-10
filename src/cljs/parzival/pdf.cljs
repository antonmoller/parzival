(ns parzival.pdf
  (:require
   [re-frame.core :refer [dispatch reg-event-fx reg-fx reg-event-db reg-sub]]
   [parzival.style :refer [PAGEMARK-COLOR HIGHLIGHT-COLOR]]
   [parzival.utils :as utils]
   ["pdfjs-dist" :as pdfjs]
   ["pdfjs-dist/web/pdf_viewer.js" :as pdfjs-viewer]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def SVG-NAMESPACE "http://www.w3.org/2000/svg")

(when (utils/electron?)
  (def path (js/require "path"))
  (def fs (js/require "fs")))

(def STROKE-WIDTH 8)
(def MIN-PX 100)

;;; Pdf

(reg-sub
 :pdf/num-pages
 :<- [:page/active]
 :<- [:pages]
 (fn [[page-uid pages]  _]
   (get-in pages [page-uid :num-pages])))

(reg-sub
 :pdf/page-quota
 :<- [:pdf/num-pages]
 (fn [num-pages _]
   (/ num-pages)))


(reg-fx
 :pdf/create
 (fn [{:keys [filename filepath data worker]}]
   (go
     (try
       (let [pdf (<p! (.-promise (.getDocument pdfjs (js-obj "data" data "worker" worker))))
             meta (<p! (.getMetadata pdf))
             title (.. meta -info -Title)
             author (.. meta -info -Author)
             num-pages (.-numPages pdf)]
         (dispatch [:page/create
                    {:title (if (not-empty title) title filename)
                     :num-pages num-pages
                     :authors (if (not-empty author) author "")
                     :filename filename}])
         (dispatch [:fs/write! filepath data])
         (<p! (.destroy (.-loadingTask pdf))))
       (catch js/Error e (js/console.log (ex-cause e)))))))

(reg-fx
 :pdf/document
 (fn [{:keys [data viewer worker]}]
   (go
     (try
       (let [obj (if (utils/electron?) 
                   (js-obj "data" data "worker" worker)
                   (js-obj "url" data "worker" worker))
             pdf (<p! (.-promise (.getDocument pdfjs obj)))]
         (.setDocument viewer pdf)
         (.setDocument (.-linkService ^js viewer) pdf nil))
       (catch js/Error e (js/console.log (ex-cause e)))))))

(reg-event-fx
 :pdf/load
 (fn [{:keys [db]} [_ uid pdf-filename]]
   (let [pdf-viewer (get db :pdf/viewer)
         pdf-worker (get db :pdf/worker)
         pdf-file (if (utils/electron?)
                    (as-> (get db :db/filepath) p
                      (.dirname path p)
                      (.resolve path p "pdfs")
                      (.resolve path p pdf-filename)
                      (.readFileSync fs p))
                    (-> (get db :db/filepath)
                        (str "/" pdf-filename)))]
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
     (.on event-bus "pagesinit" #(set! (.-currentScaleValue pdf-viewer) "page-width"))
     (.on event-bus "textlayerrendered" #(dispatch [:pdf/render-page (.. % -source -textLayerDiv -parentNode)]))
     {:db (-> db
              (assoc :pdf/viewer pdf-viewer)
              (assoc :pdf/worker (pdfjs/PDFWorker. "pdf-worker")))})))

(reg-event-fx
 :pdf/update-size
 (fn [{:keys [db]} _]
   (when-let [pdf-viewer (get db :pdf/viewer)]
     (set! (.-currentScaleValue pdf-viewer) "page-width"))
   {}))

;; TODO: Create a more general version by looking at pagemark-resize
(reg-event-fx
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
                                          (dispatch [:pdf/update-size]))
                            (js-obj "once" true)))))
   {}))

(reg-event-fx
 :pdf/render-page
 (fn [{:keys [db]} [_ page]]
   (let [page-uid (get db :page/active)
         page-num (int (.getAttribute page "data-page-number"))
         page-rect (.getBoundingClientRect (.querySelector page "canvas"))
         page-containers (.-children (.querySelector page ".textLayer"))
         svg (.createElementNS js/document SVG-NAMESPACE "svg")
         canvas-wrapper (.querySelector page ".canvasWrapper")
         pagemark (get-in db [:pages page-uid :pagemarks page-num])
         highlights (get-in db [:pages page-uid :highlights page-num])]
     (doto svg
       (.setAttribute "class" "svgLayer")
       (.setAttribute "style" "position: absolute; inset: 0; width: 100%; height: 100%;
                               mix-blend-mode: multiply; z-index: 1; pointer-events: none; 
                               overflow: visible;"))
     {:fx (cond-> [(.append canvas-wrapper svg)]
            (some? pagemark) (conj [:pagemark/render [pagemark svg]])
            (some? highlights) (concat (map (fn [v]
                                              [:highlight/render
                                               [(second v) (first v) svg page-rect page-containers]])
                                            highlights)))})))

;; Menu

(reg-sub
 :pdf/menu-anchor
 (fn [db _]
   (:pdf/menu-anchor db)))

(reg-event-db
 :pdf/menu-set-anchor
 (fn [db [_ coords]]
   (assoc db :pdf/menu-anchor coords)))

(reg-event-fx
 :pdf/menu-open
 (fn [_ [_ target x y]]
   (let [page (.closest target ".page")
         page-num (int (.getAttribute page "data-page-number"))
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
         height (if (> height-px MIN-PX)
                      (/ height-px (.-height page-rect))
                      (/ MIN-PX (.-height page-rect)))]
     {:fx [[:dispatch [:pdf/menu-set-anchor
                       {:left left
                        :top top
                        :height height
                        :edit? (not= 0 (.-length (.getElementsByClassName page "pagemark")))
                        :page-num page-num}]]]})))
                        
(reg-event-fx
 :pdf/menu-close
 (fn [_ _]
   (.addEventListener js/document "mousedown"
                      #(dispatch [:pdf/menu-set-anchor nil])
                      (js-obj "once" true))))

;;; Highlights

;; Helpers

(defn highlight-get-end
  [range-obj]
  (let [end (.-endContainer range-obj)]
    (if (= (.-nodeName end) "SPAN")
      [(.-previousSibling end)
       (.. end -previousSibling -lastChild -length)]
      [(.-parentNode end)
       (.-endOffset range-obj)])))

(defn highlight-overlapping?
  [{y0-0 :start x0-0 :start-offset y0-1 :end x0-1 :end-offset}
   {y1-0 :start x1-0 :start-offset y1-1 :end x1-1 :end-offset}]
  (or
   (and (< y0-0 y1-1) (> y0-1 y1-0))
   (and (= y0-0 y0-1 y1-0 y1-1) (and (< x0-0 x1-1) (> x0-1 x1-0)))
   (and (= y0-1 y1-0) (not= y0-0 y0-1 y1-0 y1-1) (> x0-1 x1-0))
   (and (= y0-0 y1-1) (not= y0-0 y0-1 y1-0 y1-1) (< x0-0 x1-1))))

(defn highlight-merge-two
  [{c :color y0-0 :start x0-0 :start-offset y0-1 :end x0-1 :end-offset}
   {y1-0 :start x1-0 :start-offset y1-1 :end x1-1 :end-offset}]
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

(defn highlight-merge
  [page highlight]
  (reduce-kv (fn [m k highlight]
               (if (highlight-overlapping? (:merged m) highlight)
                 (do
                   (.remove (.getElementById js/document k))
                   (assoc m :merged (highlight-merge-two (:merged m) highlight)))
                 (assoc-in m [:highlights k] highlight)))
             {:merged highlight :highlights {}}
             page))

;; CRUD

(reg-fx
 :highlight/render
 (fn [[{:keys [color start start-offset end end-offset]} highlight-uid svg page-rect rows]]
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
 :highlight/add
 (fn [{:keys [db]} [_ color]]
   (let [selection (.getSelection js/document)]
     (when-not (.-isCollapsed selection)
       (let [page-uid (get db :page/active)
             range-obj (.getRangeAt selection 0)
             start-container (.. range-obj -startContainer -parentNode)
             page      (.closest start-container ".page")
             page-id   (utils/pdf-page-num page)
             page-rect (.getBoundingClientRect (.querySelector page "canvas"))
             svg (.querySelector page ".svgLayer")
             containers (.-children (.closest start-container ".textLayer"))
             container-arr (.from js/Array containers)
             [end-container end-offset]  (highlight-get-end range-obj)
             {:keys [merged highlights]} (highlight-merge (get-in db [:pages page-uid :highlights page-id])
                                                           {:color color
                                                            :start (.indexOf container-arr start-container)
                                                            :start-offset (.-startOffset range-obj)
                                                            :end (.indexOf container-arr end-container)
                                                            :end-offset end-offset})
             merged-uid (utils/gen-uid "highlight")]
         {:db (->> (assoc highlights merged-uid merged)
                   (assoc-in db [:pages page-uid :highlights page-id]))
          :fx [(.collapse range-obj)
               [:highlight/render [merged merged-uid svg page-rect containers]]]})))))

(reg-event-fx
 :highlight/remove
 (fn [{:keys [db]} _]
   (let [{:keys [element]} (get db :highlight/selected)
         page-uid (get db :page/active)
         page (.closest element ".page")]
     {:db (update-in db [:pages page-uid :highlights (utils/pdf-page-num page)]
                     dissoc (utils/highlight-uid element))
      :fx [(.remove element)]})))

(reg-event-fx
 :highlight/edit
 (fn [{:keys [db]} [_ color]]
   (let [{:keys [element]} (get db :highlight/selected)
         page-uid (get db :page/active)
         page (.closest element ".page")]
     (set! (.. element -style -fill) (:color (color HIGHLIGHT-COLOR)))
     (set! (.. element -style -fillOpacity) (:opacity (color HIGHLIGHT-COLOR)))
     {:db (update-in db [:pages page-uid :highlights
                         (utils/pdf-page-num page)
                         (utils/highlight-uid element)]
                     assoc :color color)})))

;; Toolbar

(reg-sub
 :highlight/selected
 (fn [db _]
   (:highlight/selected db)))

(reg-event-db
 :highlight/set-selected
 (fn [db [_ m]]
   (assoc db :highlight/selected m)))

(reg-sub
 :highlight/toolbar-anchor
 (fn [db _]
   (:highlight/toolbar-anchor db)))

(reg-event-fx
 :highlight/toolbar-set-anchor
 (fn [{:keys [db]} [_ rect]]
   {:db (if (nil? rect)
          (assoc db :highlight/toolbar-anchor nil)
          (as-> (.getBoundingClientRect (.getElementById js/document "viewer")) page-rect
            (assoc db
                   :highlight/toolbar-anchor
                   {:left (+ (- (.-x rect) (.-x page-rect)) (/ (.-width rect) 2))
                    :top (- (.-bottom rect) (.-y page-rect))
                    :page-right (.-right page-rect)})))}))

(reg-event-fx
 :highlight/toolbar-close
 (fn [{:keys [db]} _]
   (as->  (:element (get db :highlight/selected)) selected-highlight
     (.addEventListener js/document "mousedown"
                        (fn []
                          (dispatch [:highlight/set-selected nil])
                          (dispatch [:highlight/toolbar-set-anchor nil])
                          (when (some? selected-highlight)
                            (set! (.. selected-highlight -style -fillOpacity)
                                  (* (.. selected-highlight -style -fillOpacity) 2))))
                        (js-obj "once" true)))))

(reg-event-fx
 :highlight/toolbar-edit
 (fn [_ [_ element]]
   (set!  (.. element -style -fillOpacity) (/ (.. element -style -fillOpacity) 2))
   {:fx [[:dispatch [:highlight/set-selected {:element element
                                              :color (.. element -style -fill)}]]
         [:dispatch [:highlight/toolbar-set-anchor (.getBoundingClientRect element)]]]}))

(reg-event-fx
 :highlight/toolbar-create
 (fn [_ _]
   (when-let [selection-rect (as-> (.getSelection js/window) selection
                               (when-not (.-isCollapsed selection)
                                 (as-> (.getRangeAt selection 0) range-obj
                                   (when-not (= (.. range-obj -commonAncestorContainer -className) "pdfViewer")
                                     (.getBoundingClientRect range-obj)))))]
     {:fx [[:dispatch [:highlight/toolbar-set-anchor selection-rect]]]})))

;;; Pagemarks

;; Helpers

;TODO
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
                              (< width-px MIN-PX) (utils/px-to-percentage page-width-px MIN-PX)
                              (> width-px page-width-px) "100%"
                              :else (utils/px-to-percentage page-width-px width-px))
                            (.-width style)))
    (set! (.-height style) (if (contains? #{"ns-resize" "nwse-resize"} cursor)
                             (cond
                               (< height-px MIN-PX) (utils/px-to-percentage page-height-px MIN-PX)
                               (> height-px page-height-px) "100%"
                               :else (utils/px-to-percentage page-height-px height-px))
                             (.-height style)))))

;TODO
(defn set-cursor
  [target x y]
  (let [b-box ^js (.getBBox target)
        width (.-width b-box)
        height (.-height b-box)]
    (cond
      (and (<= (- width STROKE-WIDTH) x width)
           (<= (- height STROKE-WIDTH) y height))
      (set! (.. target -style -cursor) "nwse-resize") ; lower right corner
      (and (<= (- width STROKE-WIDTH) x width)
           (<= STROKE-WIDTH y (- height STROKE-WIDTH)))
      (set! (.. target -style -cursor) "ew-resize") ; right
      (and (<= STROKE-WIDTH x (- width STROKE-WIDTH))
           (<= (- height STROKE-WIDTH) y height))
      (set! (.. target -style -cursor) "ns-resize") ; bottom
      :else (set! (.. target -style -cursor) "default"))))

(defn pagemark-done
  [rect width height]
  (doto rect
    (.setAttribute "class" "pagemark")
    (-> (.-style)
        (set! (str "width: " width "; height:" height
                   "; fill: none; stroke-width:" STROKE-WIDTH "; stroke:"
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

(defn svg-layer
  [page-num]
  (some-> (.querySelector js/document (str "div.page[data-page-number=\""
                                           page-num "\"][data-loaded=\"true\"]"))
          (.querySelector ".svgLayer")))

(reg-event-db
 ;"Will always be of type :done and hence have :width and :height"
 :pagemark/resize
 (fn [db [_ target]]
   (assoc-in db
             [:pages (get db :page/active) :pagemarks (utils/pdf-page-num target)]
             {:width (utils/percentage-to-float (.. target -style -width))
              :height (utils/percentage-to-float (.. target -style -height))})))

(reg-fx
 :pagemark/render
 (fn [[pagemark svg]]
   (let [rect (.createElementNS js/document SVG-NAMESPACE "rect")]
     (cond
       (every? #(contains? pagemark %) [:width :height]) (pagemark-done
                                                          rect
                                                          (utils/dec-to-percentage (:width pagemark))
                                                          (utils/dec-to-percentage (:height pagemark)))
       (:skip? pagemark) (pagemark-skip rect))
     (.append svg rect))))

(reg-fx
 :pagemark/delete-render
 (fn [svg]
   (some-> (.querySelector svg "rect.pagemark") (.remove))))

;; CRUD

(reg-event-fx
 :pagemark/create
 (fn [{:keys [db]} [_  page-num pagemark]]
   (let [page-uid (get db :page/active)
         svg (svg-layer page-num)]
     (cond-> {:db (assoc-in db [:pages page-uid :pagemarks page-num] pagemark)}
       (some? svg) (assoc :fx [[:pagemark/delete-render svg]
                               [:pagemark/render [pagemark svg]]])))))

(reg-event-fx
 :pagemark/delete
 (fn [{:keys [db]} [_ page-num]]
   (let [page-uid (get db :page/active)
         svg (svg-layer page-num)]
     (cond-> {:db (update-in db [:pages page-uid :pagemarks] dissoc page-num)}
       (some? svg) (assoc :fx [[:pagemark/delete-render svg]])))))

;; Sidebar 

; CRUD
(reg-event-fx
 :pagemark/sidebar-create
 (fn [_ [_ start-page end-page deadline]]
   {:fx (map (fn [page-num]
               [:dispatch [:pagemark/create page-num (if (empty? deadline)
                                                       {:skip? true}
                                                       {:deadline deadline})]])
             (range start-page (inc end-page)))}))

(reg-event-fx
 :pagemark/sidebar-update
 (fn [_ [_ start new-start end new-end deadline]]
   {:fx [(when (< start new-start) [:dispatch [:pagemark/sidebar-delete start (dec new-start)]])
         (when (< new-end end) [:dispatch [:pagemark/sidebar-delete (inc new-end) end]])
         [:dispatch [:pagemark/sidebar-create new-start new-end deadline]]]}))

(reg-event-fx
 :pagemark/sidebar-delete
 (fn [_ [_ start-page end-page]]
   {:fx (map (fn [page-num] [:dispatch [:pagemark/delete page-num]])
             (range start-page (inc end-page)))}))

; UI

(reg-sub
 :pagemark/sidebar
 :<- [:page/active]
 :<- [:pages]
 :<- [:pdf/num-pages]
 (fn [[page-uid pages num-pages] _]
   (->> (get-in pages [page-uid :pagemarks])
        (into (sorted-map))
        (reduce-kv
         (fn [[head & tail :as l] k {:keys [width height skip? deadline]}]
           (let [end-area (* (or width 1) (or height 1))
                 type (cond
                        skip? :skip
                        deadline :deadline
                        (and width height) :done)]
             (if (and (= (:type head) type)
                      (= (+ (:end-page head) (:end-area head)) k)
                      (or (nil? deadline) (= deadline (:deadline head))))
               (conj tail (assoc head :end-page k :end-area end-area))
               (conj l {:type type :deadline (or deadline "") :start-page k :end-page k :end-area end-area}))))
         '())
        (map #(-> (dissoc % :end-area)
                  (assoc :top (utils/top-percentage % (/ num-pages))
                         :height (utils/height-percentage % (/ num-pages))))))))
   
(reg-sub
 :pagemark/sidebar-open?
 (fn [db _]
   (:pagemark/sidebar-open? db)))

(reg-event-db
 :pagemark/sidebar-set-state
 (fn [db [_ bool]]
   (assoc db :pagemark/sidebar-open? bool)))

(reg-event-fx
 :pagemark/sidebar-toggle
 (fn [_ _]
   (.addEventListener js/document
                      "pointerdown"
                      (fn close-pagemark [e]
                        (when (nil? (.closest (.-target e) "#createPagemark"))
                          (.removeEventListener js/document "pointerdown" close-pagemark)
                          (dispatch [:pagemark/sidebar-set-state false]))))
   {:fx [[:dispatch [:pagemark/sidebar-set-state true]]]}))