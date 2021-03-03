(ns parzival.events
  (:require
   [re-frame.core :as re-frame :refer [dispatch reg-event-fx reg-cofx reg-fx inject-cofx]]
   [parzival.db :as db]
   ["pdfjs-dist" :as pdfjs]
   ["pdfjs-dist/web/pdf_viewer.js" :as pdfjs-viewer]
   [goog :as goog]
   [goog.object :as obj]
   [cljs.core.async :refer [go go-loop <! timeout]]
   [cljs.core.async.interop :refer [<p!]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

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


;TODO: Render highlights on currently visible pages
;; Get the currently visible pages
;; render function
;;; Get :pdf/higlights from app-db
;;; Use visible pages function
;;; Create wrapper container For each list of highlight coordinates, the function that puts them into the db in the first place should split multi page highlights.

;TODO: Functions needed
;TODO: Highlight function
;TODO: Get coordinates of highlights function
;TODO: Into db function
;TODO: Get from db function
;TODO: Render function
;TODO: Get current pages function

; (defn walk-selection
;   [element]
;   (let [tree-walker (.createTreeWalker js/document element (obj/get js/NodeFilter "SHOW_TEXT") nil nil)]
;     (js/console.log tree-walker)
;     (js/console.log (obj/get tree-walker "currentNode"))
;     (js/console.log (.nextNode tree-walker))
;     ; (while (.nextNode tree-walker)
;     ;   (js/console.log (.currentNode tree-walker)))
;     ))

(defn highlight-coordinates
  [page-rect highlight-container rect]
  (when (= (goog/typeOf rect) "object")
    (let [left  (- (obj/get rect "left") (obj/get page-rect "x"))
          top   (- (obj/get rect "top") (obj/get page-rect "y"))
          width (obj/get rect "width")
          height (obj/get rect "height")
          highlight (.createElement js/document "div")
          highlight-style (obj/get highlight "style")]
      (obj/set highlight-style "position" "absolute")
      (obj/set highlight-style "background-color" "rgb(255, 0, 0)") 
      (obj/set highlight-style "left" (str left "px")) 
      (obj/set highlight-style "top" (str top "px")) 
      (obj/set highlight-style "width" (str width "px")) 
      (obj/set highlight-style "height" (str height "px")) 
      (.appendChild highlight-container highlight))))

(defn get-rect
  [page-rect rect]
  {:left   (str (- (obj/get rect "left") (obj/get page-rect "x")) "px")
   :top    (str (- (obj/get rect "top") (obj/get page-rect "y")) "px")
   :width  (str (obj/get rect "width") "px")
   :height (str (obj/get rect "height") "px")
   :color  "rgb(255,0,0)"})

(re-frame/reg-event-db
  :pdf/highlight-db
  (fn [db [_ page-idx higlight-coordinates]]
    (update-in db [:pdf/highlights page-idx] highlight-coordinates)))



(reg-fx
  :pdf/highlight
  (fn [{:keys [page-idx page range-obj]}]
    (js/console.log page-idx)
    (go
      (<! (timeout 300)) ; Lazy solution to make sure the padding added in enhanced selection is gone
      (let [page-rect            (aget (.getClientRects (obj/get page "canvas")) 0)
            selection-rects      (.getClientRects range-obj)
            higlight-coordinates (map #(get-rect page-rect %) selection-rects)]
        (dispatch [:pdf/highlight-db page-idx highlight-coordinates])
        (.collapse ^js range-obj true)))))

; (reg-fx
;   :pdf/highlight
;   (fn [{:keys [page range-obj]}]
;     (go
;       (<! (timeout 300)) ; Lazy solution to make sure the padding added in enhanced selection is gone
;       (let [page-rect                 (aget (.getClientRects (obj/get page "canvas")) 0)
;             selection-rects           (.getClientRects range-obj)
;             text-layer                (obj/getValueByKeys page "textLayer" "textLayerDiv")             
;             highlight-container       (.createElement js/document "div")
;             ; coords (map selection-rects #(get-rects page-rect %))
;             highlight-container-style (obj/get highlight-container "style")]
;         (obj/set highlight-container-style "position" "absolute")
;         (obj/set highlight-container-style "cursor" "pointer")
;         (js/console.log (obj/get selection-rects "length"))
;         ; (println (map #(get-rects page-rect %) selection-rects))
;         (.appendChild text-layer highlight-container)
;         (.collapse ^js range-obj true)))))

;TODO: This will only work when selecting text within one page
;TODO: It might be better to just walk the selection and manually calculate the rects
(reg-event-fx
  :highlight
  (fn [{:keys [db]} _]
    (println "HIGHLIGHT")
    (let [selection (.getSelection js/window)] 
      (when (not= (.toString selection) "")
        (let [pdf-viewer     (get db :pdf/viewer)
              page-container (obj/getValueByKeys selection "anchorNode" "parentNode" "parentNode" "parentNode")
              page-idx       (dec (.getAttribute page-container "data-page-number"))
              page           (.getPageView pdf-viewer page-idx) 
              range-obj      (.getRangeAt selection 0)]
          {:pdf/highlight {:page-idx page-idx
                           :page page
                           :range-obj range-obj}})))))

; TODO: Search within PDF
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
                                                    ;"findController" find-controller
                                                    "textLayerMode" 2))]
  (.setViewer link-service pdf-viewer)
  (.setDocument pdf-viewer pdf)
  (.setDocument link-service pdf nil)
  {:db (assoc db :pdf/viewer pdf-viewer)}
  )))

        

        
   
