(ns parzival.events
  (:require
   [re-frame.core :as re-frame :refer [dispatch reg-event-fx reg-cofx reg-fx inject-cofx]]
   [parzival.db :as db]
   ["pdfjs-dist" :as pdfjs]
   [goog.object :as obj]
   [cljs.core.async :refer [go go-loop]]
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
    (assoc-in db [:pdf :document] pdf)))

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
        (let [pdf (<p! (obj/get (.getDocument pdfjs url) "promise"))]
          (dispatch (conj on-success pdf)))
        (catch js/Error e (dispatch (conj on-failure (ex-cause e))))))))

(reg-event-fx
  :pdf/load
  (fn [_ [_ url]]
    {:pdf/document {:url url
                    :on-success [:pdf/load-success]
                    :on-failure [:pdf/load-failure]}}))

;; There seems to be a bug in core.async that makes this necesarry to not get the warning
;; "cannot infer target type in expression"
;; Source: https://increasinglyfunctional.com/2021/01/28/clojurescript-type-hinting-core-async-gotcha.html
(defn get-viewport
  [^js page]
  (.getViewport page (js-obj "scale" 1.5)))

(reg-event-fx
 :pdf/render
 (fn [{:keys [db]} [_ active-page]]
   (let [pdf (get-in db [:pdf :document])
         num-pages (obj/get pdf "numPages")
         canvas-container (.getElementById js/document "canvas-container")]
     (go-loop [curr-page (if (> active-page 3) (- active-page 2) 1)                        
               end-page   (if (>= (+ active-page 2) num-pages) num-pages (+ active-page 2))]
              (when (<= curr-page end-page)
                (let [page (<p! (.getPage pdf curr-page))
                      viewport (get-viewport page)
                      wrapper  (.createElement js/document "div") ;
                      canvas  (.createElement js/document "canvas");
                      context  (.getContext canvas "2d")
                      render-context (js-obj "canvasContext" context "viewport" viewport)]
                  (obj/set canvas "height" (obj/get viewport "height"));
                  (obj/set canvas "width" (obj/get viewport "width"));
                  (.appendChild wrapper canvas);
                  (.appendChild canvas-container wrapper);
                  (<p! (obj/get (.render page render-context) "promise")))
                (recur (inc curr-page)  end-page))))))
