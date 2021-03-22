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

; (reg-event-fx
;   :render/page
;   (fn [{:keys [db]} [_ page-idx text-layer]]
;     (run! #(render-highlight (second %) text-layer) 
;           (get-in db [:pdf/highlights page-idx]))))


; (defn next-node
;   [node]
;   (if (some? (obj/get node "nextSibling"))
;     (obj/get node "nextSibling")
;     (obj/getValueByKeys node "parentNode" "nextSibling" "firstChild")))

; (defn reduce-down
;   [f aux start-node end-node]
;   (loop [node start-node
;          res aux]
;     (if (.isSameNode node end-node)
;       (f res node)
;       (recur (next-node node) 
;              (f res node)))))

; (defn apply-fn
;   [f start-node end-node]
;   (loop [node start-node]
;     (if (.isSameNode node end-node)
;       (f node)
;       (do
;         (f node)
;         (recur (next-node node))))))

;     ; (when (not (.isSameNode node end-node))
;     ;   (recur (next-node node)))))

; (defn text-length
;   [node]
;   (if (= (obj/get node "nodeName") "SPAN")
;     (obj/getValueByKeys node "firstChild" "length")
;     (obj/get node "length")))

; (defn get-offset
;   [aux row end-node]
;   (reduce-down (fn [res node] 
;                  (+ res (text-length node)))
;                aux 
;                (obj/get row "firstChild") 
;                end-node))

; (defn highlight-node
;   [node color id]
;   (let [highlight (.createElement js/document "span")]
;     (.setAttribute highlight "class" "highlight")
;     (.setAttribute highlight "name" id)
;     ; (.setAttribute highlight "style" (str "border-radius: 0; ;FIXME: Custom style
;     ;                              cursor: pointer;
;     ;                              background-color: " color ";"))
;     (.after node highlight)
;     (.appendChild highlight node)))

; (defn highlight-selection
;   [start-node end-node color id]
;   (apply-fn #(highlight-node % color id) start-node end-node))

; (defn get-overlapping
;   [start-node end-node]
;   (reduce-down (fn [res node]
;                  (if (= (obj/get node "nodeName") "SPAN")
;                    (conj res (.getAttribute node "name"))
;                    res))
;                #{}
;                start-node
;                end-node))

; (defn split-node
;   [node start-offset end-offset]
;   (if (= (obj/getValueByKeys node "parentNode""parentNode" "nodeName") "DIV") 
;     (cond
;       (and (some? start-offset) (== 0 start-offset)) node
;       (some? start-offset) (.splitText node start-offset)
;       (some? end-offset) (-> (.splitText node end-offset)
;                              (obj/get "previousSibling")))
;     (obj/get node "parentNode"))) ; Already wrapped so return surrounding node

; (defn bounding-container
;   [node start-offset end-offset]
;   (cond
;     (some? start-offset) (split-node node start-offset nil)
;     (some? end-offset) (if (== end-offset 0)
;                          (obj/getValueByKeys node "previousSibling" "lastChild") 
;                          (split-node node nil end-offset))))

; (defn get-node
;   [row start-offset end-offset cmp-fn split?]
;   (js/console.log start-offset)
;   (loop [node (obj/get row "firstChild")
;          offset (if (some? start-offset) start-offset end-offset)]
;     (js/console.log node)
;     (js/console.log offset)
;     (if (cmp-fn 0 (- offset (text-length node)))
;       (if split?
;         (bounding-container node
;                             (if (some? start-offset) offset nil)
;                             (if (some? end-offset) offset nil))
;         node)
;       (recur (obj/get node "nextSibling") (- offset (text-length node))))))

; (defn merge-highlight
;   [[page {:keys [color a0 a1 b0 b1]} highlight] k]
;     (let [{:keys [_ y0 y1 x0 x1]} (get page k)
;           start (if (<= a0 y0) [a0 b0] [y0 x0])
;           end   (if (<= a1 y1) [a1 b1] [y1 x1])]
;       [(dissoc page k)
;        {:start-id (first start)
;         :end-id (first end)
;         :start-offset (if (== a0 y0) (min b0 x0) (second start))
;         :end-offset (if (== a1 y1) (min b1 x1) (second end))
;         :color color}]))

; (defn create-key
;   [page-id highlight]
;   (str "highlight-" page-id "-" 
;        (:start-id highlight) "-"
;        (:end-id highlight) "-"
;        (:start-offset highlight) "-"
;        (:end-offset highlight)))

; (defn bound-container
;   [node]
;   (if (= (obj/get node "parentNode" "parentNode" "nodeName") "DIV")
;     node
;     (obj/get "parentNode")))



(defn next-node
  [node]
  (if (some? (obj/get node "nextSibling"))
    (obj/get node "nextSibling")
    (obj/getValueByKeys node "parentNode" "nextSibling" "firstChild")))

(defn reduce-down
  [start-node end-node start-offset end-offset range-obj]
  (.setStart range-obj start-node start-offset)
  (loop [node (next-node start-node)]
    (if (.isSameNode node end-node)
      (.setEnd range-obj end-node end-offset)
      (do
        (.selectNodeContents range-obj (obj/get node "parentNode"))
        (recur (next-node node))))))

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


(defn get-highlight
  [range-obj page-rect viewport color]
  (let [start-row (obj/getValueByKeys range-obj "startContainer" "parentNode")
        end-row   (obj/getValueByKeys range-obj "endContainer" "parentNode")
        start-offset (obj/get range-obj "startOffset")
        end-offset   (obj/get range-obj "endOffset")
        r         (js/Range.)]
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
  (let [parent (.createElement js/document "div")]
    (.setAttribute parent "style" "cursor: pointer; position: absolute;")
    (doseq [rect coords]
      (let [[b0 b1 b2 b3] ^js (.convertToViewportRectangle viewport rect)
            child (.createElement js/document "div")]
        (.setAttribute child "style" (str "position: absolute; background-color: " color
                                          "; left: " (min b0 b2) "px; top: " (min b1 b3) 
                                          "px; width: " (Math/abs (- b0 b2)) 
                                          "px; height: " (Math/abs (- b1 b3)) "px;"))
        (.append parent child)))
    (.append text-layer parent)))




;TODO: {:color :coords {:x :y :w :h}}
;TODO: zero end-offset bug
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
          ; (loop [node start]
          ;   (js/console.log node)
          ;   (.setStart r node (if (.isSameNode node start) (obj/get range-obj "startOffset") 0))
          ;   (.setEnd r node (if (.isSameNode node end) (obj/get range-obj "endOffset") (obj/get node "length")))
          ;   (js/console.log (.getBoundingClientRect r))
          ;   (if (not (.isSameNode node end))
          ;     (recur (next-node node))))

          ; (js/console.log (loop [node start]
          ;   (js/console.log node)
          ;   (if (.isSameNode node end)
          ;     node
          ;     (obj/get node "nextSibling"))))

          ; (js/console.log (reduce-down (obj/get range-obj "startContainer")
          ;                              (obj/get range-obj "endContainer")
          ;                              (obj/get range-obj "startOffset")
          ;                              (obj/get range-obj "endOffset")
          ;                              new-range))
          ; (js/console.log (.getClientRects new-range))
          
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
