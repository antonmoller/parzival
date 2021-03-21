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


(defn next-node
  [node]
  (if (some? (obj/get node "nextSibling"))
    (obj/get node "nextSibling")
    (obj/getValueByKeys node "parentNode" "nextSibling" "firstChild")))

(defn reduce-down
  [f aux start-node end-node]
  (loop [node start-node
         res aux]
    (if (.isSameNode node end-node)
      (f res node)
      (recur (next-node node) 
             (f res node)))))

(defn apply-fn
  [f start-node end-node]
  (loop [node start-node]
    (if (.isSameNode node end-node)
      (f node)
      (do
        (f node)
        (recur (next-node node))))))

    ; (when (not (.isSameNode node end-node))
    ;   (recur (next-node node)))))

(defn text-length
  [node]
  (if (= (obj/get node "nodeName") "SPAN")
    (obj/getValueByKeys node "firstChild" "length")
    (obj/get node "length")))

(defn get-offset
  [aux row end-node]
  (reduce-down (fn [res node] 
                 (+ res (text-length node)))
               aux 
               (obj/get row "firstChild") 
               end-node))

(defn highlight-node
  [node color id]
  (let [highlight (.createElement js/document "span")]
    (.setAttribute highlight "class" "highlight")
    (.setAttribute highlight "name" id)
    ; (.setAttribute highlight "style" (str "border-radius: 0; ;FIXME: Custom style
    ;                              cursor: pointer;
    ;                              background-color: " color ";"))
    (.after node highlight)
    (.appendChild highlight node)))

(defn highlight-selection
  [start-node end-node color id]
  (js/console.log start-node end-node color id)
  (apply-fn #(highlight-node % color id) start-node end-node))

(defn get-overlapping
  [start-node end-node]
  (reduce-down (fn [res node]
                 (if (= (obj/get node "nodeName") "SPAN")
                   (conj res (.getAttribute node "name"))
                   res))
               #{}
               start-node
               end-node))

(defn split-node
  [node start-offset end-offset]
  (if (= (obj/getValueByKeys node "parentNode""parentNode" "nodeName") "DIV") 
    (cond
      (and (some? start-offset) (== 0 start-offset)) node
      (some? start-offset) (.splitText node start-offset)
      (some? end-offset) (-> (.splitText node end-offset)
                             (obj/get "previousSibling")))
    (obj/get node "parentNode"))) ; Already wrapped so return surrounding node

(defn bounding-container
  [node start-offset end-offset]
  (cond
    (some? start-offset) (split-node node start-offset nil)
    (some? end-offset) (if (== end-offset 0)
                         (obj/getValueByKeys node "previousSibling" "lastChild") 
                         (split-node node nil end-offset))))

(defn get-node
  [row start-offset end-offset]
  (loop [node (obj/get row "firstChild")
         offset (if (some? start-offset) start-offset end-offset)]
    (if (>= 0 (- offset (text-length node)))
      (bounding-container node
                          (if (some? start-offset) offset nil)
                          (if (some? end-offset) offset nil))
      (recur (obj/get node "nextSibling") (- offset (text-length node))))))

(defn merge-highlight
  [[page {:keys [color a0 a1 b0 b1]} highlight] k]
    (let [{:keys [_ y0 y1 x0 x1]} (get page k)
          start (if (<= a0 y0) [a0 b0] [y0 x0])
          end   (if (<= a1 y1) [a1 b1] [y1 x1])]
      [(dissoc page k)
       {:start-id (first start)
        :end-id (first end)
        :start-offset (if (== a0 y0) (min b0 x0) (second start))
        :end-offset (if (== a1 y1) (min b1 x1) (second end))
        :color color}]))

(defn create-key
  [page-id highlight]
  (str "highlight-" page-id "-" 
       (:start-id highlight) "-"
       (:end-id highlight) "-"
       (:start-offset highlight) "-"
       (:end-offset highlight)))

(reg-event-fx
  :highlight
  (fn [{:keys [db]} [_ color _ id]]
    (let [selection (.getSelection js/document)] 
      (when-not (obj/get selection "isCollapsed")
        (let [range-obj    (.getRangeAt selection 0)
              start-node   (bounding-container (obj/get range-obj "startContainer") 
                                               (obj/get range-obj "startOffset") 
                                               nil)
              end-node     (bounding-container (obj/get range-obj "endContainer") 
                                               nil 
                                               (obj/get range-obj "endOffset"))
              start-row   (obj/get start-node "parentNode")
              end-row   (obj/get end-node "parentNode")
              text-layer  (obj/getValueByKeys start-node "parentNode" "parentNode")
              rows    (.from js/Array (obj/get text-layer "children"))
              page-id   (-> (obj/get text-layer "parentNode")
                            (.getAttribute "data-page-number")
                            (dec))
              highlight {:color color
                         :start-id (.indexOf rows start-row)
                         :end-id (.indexOf rows end-row)
                         :start-offset (get-offset (- (text-length start-node)) start-row start-node)
                         :end-offset (get-offset 0 end-row end-node)}
              [new-page new-highlight] (->> (get-overlapping start-node end-node)
                                            (reduce merge-highlight [(get-in db [:pdf/highlights page-id]) highlight]))]
          (.empty selection)
          (js/console.log
            (get-node (aget rows (:start-id new-highlight)) (:start-offset new-highlight) nil))
          (js/console.log
            (get-node (aget rows (:end-id new-highlight)) nil (:end-offset new-highlight)))
          ; {:db (->> (assoc new-page (create-key page-id new-highlight) new-highlight)
          ;           (assoc-in db [:pdf/highlights page-id]))
          ;  :fx [(highlight-selection (get-node (aget rows (:start-id new-highlight)) (:start-offset new-highlight) nil)
          ;                            (get-node (aget rows (:end-id new-highlight)) nil (:end-offset new-highlight))
          ;                  color 
          ;                  id)]}
          ; (js/console.log (reduce merge-highlight highlight to-merge))
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
