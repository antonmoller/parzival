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
  [[node skip-children]]
  (cond
    (and (some? (obj/get node "firstChild")) (not skip-children)) [(obj/get node "firstChild") false]
    (some? (obj/get node "nextSibling")) [(obj/get node "nextSibling") false]
    :else [(obj/get node "parentNode") true]))

(defn reduce-dom
  [f aux start-node end-node]
  (loop [[node skip-children :as it] [start-node false]
         res aux]
    (if (.isSameNode node end-node) ;FIXME End condition instead?
      res
      (recur (next-node it)
             (f res node)))))

; walk upward until the next parent name is a DIV
(defn get-row
  [start-node]
  (loop [node start-node]
    (if (= (obj/getValueByKeys node "parentNode" "nodeName") "DIV")
      node
      (recur (obj/get node "parentNode")))))


;#(.isSameNode %1 end-node)
(defn get-offset
  [aux row end-node]
  (reduce-dom (fn [res node] 
                (if (= (obj/get node "nodeName") "#text") 
                  (+ res (obj/get node "length")) 
                  res))
              aux 
              (obj/get row "firstChild") 
              end-node))

(defn reduce-ha
  [f aux start-node end-node]
  (loop [[node skip-children :as it] [start-node false]
         res aux]
    (if (.isSameNode node end-node)
      (f res node)
      (recur (next-node it)
             (f res node)))))

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
      (.surroundContents highlight))))

; walk and surround each text-node with a highlight
; add span nodes that has a name to a to-merge and return this
(defn highlight-and-overlapping
  [start-node end-node start-offset end-offset color id]
  (reduce-ha (fn [res node]
                (if (= (obj/get node "nodeName") "#text")
                  (do
                    (highlight-node node 
                                    (if (.isSameNode node start-node) start-offset 0) 
                                    (if (.isSameNode node end-node) end-offset (obj/get node "length")) 
                                    color
                                    id)
                    res)
                    (if-let [nam (.getAttribute node "name")]
                      (conj res (.getAttribute node "name"))
                      res)))
                #{}
                start-node
                end-node))


(reg-event-fx
  :highlight
  (fn [{:keys [db]} _]
    (let [selection (.getSelection js/document)] 
      (when-not (obj/get selection "isCollapsed")
        (let [range-obj (.getRangeAt selection 0)
              start-node   (obj/getValueByKeys range-obj "startContainer")
              end-node     (obj/getValueByKeys range-obj "endContainer")
              start-row (get-row start-node)
              end-row   (get-row end-node)
              text-layer   (obj/get start-row "parentNode")
              children     (.from js/Array (obj/get text-layer "children"))
              page-id      (-> (obj/get text-layer "parentNode")
                               (.getAttribute "data-page-number")
                               (dec))
              ; fragment (.cloneContents range-obj)
              highlight    {:color        "rgb(0,100,0)"
                            :start-idx    (.indexOf children start-row)
                            :end-idx      (.indexOf children end-row)
                            :start-offset (get-offset (obj/get range-obj "startOffset") start-row start-node)
                            :end-offset   (get-offset (obj/get range-obj "endOffset") end-row end-node)}]

          (js/console.log highlight)
          ; (js/console.log fragment)
          (js/console.log (highlight-and-overlapping start-node 
                                                     end-node 
                                                     (obj/get range-obj "startOffset") 
                                                     (obj/get range-obj "endOffset")
                                                     "rgb(0,100,0)"
                                                     "testing"))
          (.empty selection)

          ; (.setAttribute span "class" "highlight")
          ; (js/console.log start-node end-node)
          ; (js/console.log (obj/get selection "anchorOffset") (obj/get selection "focusOffset"))
          ; (js/console.log (obj/get start-node "offsetLeft") (obj/get end-node "offsetRight"))
          ; (js/console.log fragment)
          ; (js/console.log (obj/get fragment "childNodes"))
          ; (.forEach (.querySelectorAll text-layer "span[name='test']") (fn [v k l] (.setAttribute v "class" "highlight")))
          ; (.forEach (obj/get fragment "childNodes") (fn [v k l] 
          ; (.forEach (obj/get fragment "childNodes") (fn [v k l] 
          ; (.forEach (.getElementsByTagName text-layer "span") (fn [v k l] (.setAttribute v "class" "highlight")))


          ; (.forEach (obj/get fragment "childNodes") (fn [v k l] 
          ;                                             (let [style (obj/get v "style")]
          ;                                               (obj/set style "padding" 0)
          ;                                               (obj/set style "background-clip" "content-box")
          ;                                               (obj/set style "transform" "none")
          ;                                             ; (.setAttribute v "background-clip" "content-box")
          ;                                             (.setAttribute v "class" "highlight")
          ;                                               )))
          ; ; (js/console.log (obj/getValueByKeys fragment "lastChild" "nextSibling"))
          ; (js/console.log fragment)
          ; (.surroundContents rang span)
          ; (js/console.log (.deleteContents rang))
          ; (js/console.log (.appendChild text-layer fragment))
          ; (.insertBefore text-layer fragment)


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
