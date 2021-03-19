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

; (defn find-node
;   [row offset]
;   (loop [node (obj/get row "firstChild")
;          relative-offset offset]
;     (if (>= 0 (- relative-offset (text-length node)))
;       [node relative-offset]
;       (recur (obj/get node "nextSibling") (- relative-offset (text-length node))))))

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

(defn walk
  [start-node end-node]
  (loop [node start-node]
    (js/console.log node)
    (if (.isSameNode node end-node)
      (recur (next-node node)))))

; walk upward until we reach the text-layer
(defn get-row
  [start-node]
  (loop [node start-node]
    (if (= (obj/getValueByKeys node "parentNode" "nodeName") "DIV")
      node
      (recur (obj/get node "parentNode")))))

(defn text-length
  [node]
  (if (= (obj/get node "nodeName") "SPAN")
    (obj/getValueByKeys node "firstChild" "length")
    (obj/get node "length")))

(defn get-offset
  [aux row end-node]
  (-> (reduce-down (fn [res node] 
                     (+ res (text-length node)))
                   aux 
                   (obj/get row "firstChild") 
                   end-node)
      (- (text-length end-node))))

(defn highlight-node
  [node start-offset end-offset color id]
  (let [highlight    (.createElement js/document "span")]
    (.setAttribute highlight "class" "highlight")
    (.setAttribute highlight "style" (str "border-radius: 0;
                                           cursor: pointer;
                                           background-color: " color ";"))
    (.setAttribute highlight "name" id)
    (doto (js/Range.)
      (obj/get "commonAncestorContainer" node)
      (.setStart node start-offset)
      (.setEnd node end-offset)
      (.surroundContents highlight))
    (.normalize (obj/getValueByKeys node "parentNode" "parentNode"))))

(defn highlight-and-overlapping
  [start-node end-node start-offset end-offset color id]
  (reduce-down (fn [res node]
                 (if (= (obj/get node "nodeName") "SPAN")
                  (let [name-id (.getAttribute node "name")]
                    (.setAttribute node "name" id)
                    (conj res name-id))
                  (do
                    (highlight-node node
                                    (if (.isSameNode node start-node) start-offset 0)
                                    (if (.isSameNode node end-node) end-offset (text-length node))
                                    color 
                                    id)
                    res)))
               #{}
               start-node
               end-node))

(defn bounding-container
  [row node]
  (if (.isSameNode (obj/get node "parentNode") row)
                               node
                               (obj/get node "parentNode")))

(reg-event-fx
  :highlight
  (fn [{:keys [db]} [_ color]]
    (let [selection (.getSelection js/document)] 
      (when-not (obj/get selection "isCollapsed")
        (let [range-obj    (.getRangeAt selection 0)
              start        (obj/get range-obj "startContainer")
              end          (obj/get range-obj "endContainer")
              start-row    (get-row start)
              end-row      (get-row end)
              start-node   (bounding-container start-row start)
              end-node     (bounding-container end-row end)
              text-layer   (obj/get start-row "parentNode")
              children     (.from js/Array (obj/get text-layer "children"))
              page-id      (-> (obj/get text-layer "parentNode")
                               (.getAttribute "data-page-number")
                               (dec))
              highlight    {:color        color
                            :start-idx    (.indexOf children start-row)
                            :end-idx      (.indexOf children end-row)
                            :start-offset (get-offset (obj/get range-obj "startOffset") start-row start-node)
                            :end-offset   (get-offset (obj/get range-obj "endOffset") end-row end-node)}
              ]
          (js/console.log (highlight-and-overlapping 
                            start-node 
                            end-node 
                            (obj/get range-obj "startOffset")
                            (obj/get range-obj "endOffset")
                            color
                            "testing"))
          (.empty selection)
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
