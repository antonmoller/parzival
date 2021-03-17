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


; walk upward until the next parent name is a DIV
(defn get-parent
  [start-node]
  (loop [node start-node]
    (if (= (obj/getValueByKeys node "parentNode" "nodeName") "DIV")
      node
      (recur (obj/get node "parentNode")))))

(defn text-length
  [node]
  (case (obj/get node "nodeName")
    "#text" (obj/get node "length")
    "SPAN"  (obj/getValueByKeys node "firstChild" "length")))

; (defn render-dev
;   [{:keys [color start-idx end-idx start-offset end-offset]} text-layer]
;     (doseq [node (range start-idx (inc end-idx))]
      
; (reg-event-fx
;   :render/page
;   (fn [{:keys [db]} [_ page-idx text-layer]]
;     (run! #(render-highlight (second %) text-layer) 
;           (get-in db [:pdf/highlights page-idx]))))

(defn same-node
  [x y] ; y will always be a text node
  (let [node (if (= (obj/get x "nodeName") "SPAN") (obj/get x "firstChild") x)]
    (.isSameNode node y)))

(defn get-offset
  [aux row-container end-node]
  (loop [node (obj/get row-container "firstChild")
         res aux]
    (if (same-node node end-node)
      res
      (recur (obj/get node "nextSibling") (+ res (text-length node))))))

(defn highlight?
  [node]
  (= (obj/get node "nodeName") "SPAN"))

(defn find-node
  [row offset]
  (loop [node (obj/get row "firstChild")
         relative-offset offset]
    (if (>= 0 (- relative-offset (text-length node)))
      [node relative-offset]
      (recur (obj/get node "nextSibling") (- relative-offset (text-length node))))))

(defn render-row
  [color id row start end]
  (let [[old-child start-offset] (if (some? start) (find-node row start) [(obj/get row "firstChild") 0])
        [_ end-offset] (if (some? end) 
                         (find-node row end) 
                         [nil (obj/get old-child "length")]) ;FIXME: Something is very wrong here
        new-child (.createElement js/document "span")]
    (.setAttribute new-child "class" "highlight")
    (.setAttribute new-child "style" (str "border-radius: 0; 
                                           cursor: pointer;
                                           background-color: " color ";"))
    (.setAttribute new-child "name" id)
    (doto (js/Range.)
      (obj/set "commonAncestorContainer" old-child)
      (.setStart old-child start-offset)
      (.setEnd old-child end-offset)
      (.surroundContents new-child))))

; bounding highlights fucks things up:w

(defn render-dev
  [{:keys [color start-idx end-idx start-offset end-offset]} id text-layer]
    (doseq [node (range start-idx (inc end-idx))]
      (render-row color
                  id
                  (aget text-layer node)
                  (if (= node start-idx) start-offset nil)
                  (if (= node end-idx) end-offset nil))))


; (defn row-overlapping
;   [aux start-node end-node]
;   (loop [node start-node
;          res aux]
;     (if (or (nil? node) (same-node node end-node))
;       res
;       (recur (obj/get node "nextSibling") ()


; (defn find-overlapping
;   [start-parent end-parent start-node end-node]
;   (loop [row start-parent
;          to-merge #{}]
;     (if (.isSameNode row (obj/get end-parent "nextSibling"))
;       to-merge
;       (recur (obj/get end-parent "nextSibling")
;              (row-overlapping to-merge
;                               row
;                               (if (.isSameNode row start-parent) 
;                                 start-node 
;                                 (obj/get row "firstChild"))
;                               (if (.isSameNode row end-parent) 
;                                 start-parent 
;                                 (obj/get row "lastChild")))))))

(reg-event-fx
  :highlight
  (fn [{:keys [db]} _]
    (let [selection (.getSelection js/document)] 
      (when (not (obj/get selection "isCollapsed"))
        (let [start-node   (obj/getValueByKeys selection "anchorNode")
              end-node     (obj/getValueByKeys selection "focusNode")
              start-parent (get-parent start-node)
              end-parent   (get-parent end-node)
              text-layer   (obj/get start-parent "parentNode")
              page-id      (-> (obj/get text-layer "parentNode")
                               (.getAttribute "data-page-number")
                               (dec))
              children     (.from js/Array (obj/get text-layer "children"))
              highlight    {:color        "rgb(0,100,0)"
                            :start-idx    (.indexOf children start-parent)
                            :end-idx      (.indexOf children end-parent)
                            :start-offset (get-offset (obj/get selection "anchorOffset") start-parent start-node)
                            :end-offset   (get-offset (obj/get selection "focusOffset") end-parent end-node)}
              ]
          ; (js/console.log start-offset end-offset)
          ; (js/console.log start-node)
          ; (js/console.log end-node)
          (js/console.log selection)
          (js/console.log (.getRangeAt selection 0))
          (js/console.log highlight)
          (js/console.log (render-dev highlight (str (:start-idx highlight) "-" (:start-offset highlight)) children))
          ; (js/console.log (find-intersect start-parent end-parent start-node end-node))
          ; (render-dev highlight children)
          ; (.empty selection)

;          {:db (assoc-in db [:pdf/highlights page-id] 
;                         (assoc new-map (str (:start-idx new-highlight) "-" (:start-offset new-highlight)) new-highlight))
;           :fx [(render-highlight new-highlight children)]} ;add remove-highlights
;          )))))


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
  ; (.on event-bus "textlayerrendered" 
  ;                #(dispatch [:render/page (dec (obj/get % "pageNumber"))
  ;                                         (obj/getValueByKeys % "source" "textDivs")]))
  {:db (assoc db :pdf/viewer pdf-viewer)})))
