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

(defn highlight-node
  [color node start-offset end-offset]
  (let [old-child (obj/get node "firstChild")
        new-child (.createElement js/document "span")]
    (.setAttribute new-child "class" "highlight selected")
    (.setAttribute new-child "style" (str "border-radius: initial; 
                                           cursor: pointer;
                                           background-color: " color ";"))
    (doto (js/Range.)
      (obj/set "commonAncestorContainer" old-child)
      (.setStart old-child start-offset)
      (.setEnd old-child (if (nil? end-offset)
                           (obj/get old-child "length")
                           end-offset))
      (.surroundContents new-child))))

(defn render-highlight
  [{:keys [color start-idx end-idx start-offset end-offset]} text-layer]
    (doseq [node (range start-idx (inc end-idx))]
        (highlight-node color 
                        (aget text-layer node) 
                        (if (= node start-idx) start-offset 0) 
                        (if (= node end-idx) end-offset nil))))

(reg-event-fx
  :render/page
  (fn [{:keys [db]} [_ page-idx text-layer]]
    (run! #(render-highlight (second %) text-layer) 
          (get-in db [:pdf/highlights (dec page-idx)]))))
               
;TODO: Check if start page and end page are the same
;TODO: Don't allow double highlights
;TODO: Highlighting over something merges the highlights
; This means there will only be one highlight node within a div
(reg-event-fx
  :highlight
  (fn [{:keys [db]} _]
    (let [selection (.getSelection js/document)] 
      (when (not (obj/get selection "isCollapsed"))
        (let [start-node     (obj/getValueByKeys selection "anchorNode" "parentNode")
              end-node       (obj/getValueByKeys selection "focusNode" "parentNode")
              text-layer     (obj/get start-node "parentNode")
              page-id        (-> (obj/get text-layer "parentNode")
                                 (.getAttribute "data-page-number")
                                 (dec))
              children       (.from js/Array (obj/get text-layer "children"))
              highlight      {:color        "rgb(0,100,0)" 
                              :start-idx    (.indexOf children start-node)
                              :end-idx      (.indexOf children end-node)
                              :start-offset (obj/get selection "anchorOffset")
                              :end-offset   (obj/get selection "focusOffset")}]
          (.empty selection)
          {:db (if-let [page-highlights (get-in db [:pdf/highlights page-id])]
                 (update-in db [:pdf/highlights page-id] assoc (count page-highlights) highlight)
                 (assoc-in db [:pdf/highlights page-id 0] highlight))
           :fx [(render-highlight highlight children)]})))))

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
                 #(dispatch [:render/page (obj/get % "pageNumber")
                                          (obj/getValueByKeys % "source" "textDivs")]))
  {:db (assoc db :pdf/viewer pdf-viewer)})))
