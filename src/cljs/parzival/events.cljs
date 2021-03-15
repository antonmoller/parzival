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

(defn text-length
  [node]
  (js/console.log node)
  (case (obj/get node "nodeName")
    "#text" (obj/get node "length")
    "SPAN"  (obj/getValueByKeys node "firstChild" "length")))

; ; This only needs to be done on a start-node or a end-node or one that is both
; ; This is because no nodes can overlap. This is handeled by the highlight function
(defn find-node
  [start-node start-offset]
  (if (nil? start-offset)
    [start-node nil]
    (loop [node start-node
           offset start-offset]
      (if (>= 0 (- offset (text-length node)))
        [node offset]
        (recur (obj/get node "nextSibling") (- offset (text-length node)))))))

; Walk the list of child nodes, once the correct text-node is found split do the range thing
; The offsets will probably need modification
;TODO: Calculate new start-offset and end-offset
(defn highlight-node
  [color node parent-s parent-e]
  (let [[old-child start-offset] (find-node (obj/get node "firstChild") parent-s) ; The child node we want to highlight
        [_ end-offset] (find-node (obj/get node "firstChild") parent-e)
        new-child (.createElement js/document "span")]
    (.setAttribute new-child "class" "highlight selected")
    (.setAttribute new-child "style" (str "border-radius: initial; 
                                           cursor: pointer;
                                           background-color: " color ";"))
    (doto (js/Range.)
      (obj/set "commonAncestorContainer" old-child)
      (.setStart old-child (if (nil? start-offset) ;TODO: Offset wrong
                             0
                             start-offset))
      (.setEnd old-child (if (nil? end-offset)  ;TODO: Offset wrong
                           (obj/get old-child "length")
                           end-offset))
      (.surroundContents new-child))))

(defn render-highlight
  [{:keys [color start-idx end-idx start-offset end-offset]} text-layer]
    (doseq [node (range start-idx (inc end-idx))]
        (highlight-node color 
                        (aget text-layer node) 
                        (if (= node start-idx) start-offset nil) 
                        (if (= node end-idx) end-offset nil))))

(reg-event-fx
  :render/page
  (fn [{:keys [db]} [_ page-idx text-layer]]
    (run! #(render-highlight (second %) text-layer) 
          (get-in db [:pdf/highlights page-idx]))))

(defn merge-if-overlapping
  [m k v]
  (let [h (get m :new-highlight)]
    (if (or (and (< (:start-idx h) (:end-idx v)) (< (:start-idx v) (:end-idx h))) ;TODO refractor into function with x and y
            (and (== (:start-idx h) (:end-idx v)) (<= (:start-offset h) (:end-offset v)))
            (and (== (:start-idx v) (:end-idx h)) (<= (:start-offset v) (:end-offset h))))
      (let [start        (if (<= (:start-idx h) (:start-idx v)) [(:start-idx h) (:start-offset h)] [(:start-idx v) (:start-offset v)]) 
            end          (if (>= (:end-idx h) (:end-idx v)) [(:end-idx h) (:end-offset h)] [(:end-idx v) (:end-offset v)])
            start-offset (if (== (:start-idx h) (:start-idx v)) (min (:start-offset h) (:start-offset v)) (second start))
            end-offset   (if (== (:end-idx h) (:end-idx v)) (max (:end-offset h) (:end-offset v)) (second end))]
        (assoc m :new-highlight
               {:color (:color h)
                :start-idx (first start)
                :end-idx (first end)
                :start-offset start-offset 
                :end-offset end-offset})
        (assoc m :delete conj v))
      (assoc-in m [:new-map k] v))))

(defn merge-overlapping
  [new-highlight page-highlights]
  (if (nil? page-highlights)
    {:new-map {(str (:start-idx new-highlight) "-" (:start-offset new-highlight)) new-highlight}
     :new-highlight new-highlight 
     :delete '()}
     (reduce-kv merge-if-overlapping 
                {:new-map {} :new-highlight new-highlight :delete '()} 
                page-highlights)))

; walk upward until the next parent name is a DIV
(defn get-parent
  [start-node]
  (loop [node start-node]
    (if (= (obj/getValueByKeys node "parentNode" "nodeName") "DIV")
      node
      (recur (obj/get node "parentNode")))))

; walk the parent children until the node is reached and then add offset
(defn get-offset
  [start-node end-node]
  (loop [node start-node
         new-offset 0]
    (if (.isSameNode node end-node)
      new-offset
      (recur (obj/get node "nextSibling") (+ new-offset (text-length node))))))
               
;FIXME: The index should be the start index and start offset as a string "index-offset"
;TODO: Check if start page and end page are the same
;TODO: Highlighting over something merges the highlights
;TODO: redraw or draw if no overlapping
;FIXME FIXME FIXME The problem now is that I need to remove delete before rendering and probably more
;Highlighting delimiting character
(reg-event-fx
  :highlight
  (fn [{:keys [db]} _]
    (let [selection (.getSelection js/document)] 
      (when (not (obj/get selection "isCollapsed"))
        (let [start-node   (obj/getValueByKeys selection "anchorNode")
              end-node     (obj/getValueByKeys selection "focusNode")
              start-parent (get-parent start-node)
              end-parent    (get-parent end-node)
              start-offset (+ (obj/get selection "anchorOffset")
                              (get-offset (obj/get start-parent "firstChild") start-node))
              end-offset   (+ (obj/get selection "focusOffset")
                              (get-offset (obj/get end-parent "firstChild") end-node))
              text-layer   (obj/get start-parent "parentNode")
              page-id      (-> (obj/get text-layer "parentNode")
                               (.getAttribute "data-page-number")
                               (dec))
              children     (.from js/Array (obj/get text-layer "children"))
              {:keys [new-map new-highlight delete]}   (merge-overlapping {:color "rgb(0, 100, 0)"
                                                                           :start-idx (.indexOf children start-parent)
                                                                           :end-idx (.indexOf children end-parent)
                                                                           :start-offset start-offset
                                                                           :end-offset end-offset}
                                                                           (get-in db [:pdf/highlights page-id]))]
         (.empty selection)
          {:db (assoc-in db [:pdf/highlights page-id] 
                         (assoc new-map (str (:start-idx new-highlight) "-" (:start-offset new-highlight)) new-highlight))
           :fx [(render-highlight new-highlight children)]} ;add remove-highlights
          )))))

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
                                          (obj/getValueByKeys % "source" "textDivs")]))
  {:db (assoc db :pdf/viewer pdf-viewer)})))
