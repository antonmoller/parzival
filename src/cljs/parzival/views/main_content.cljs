(ns parzival.views.main-content
  (:require
            ["pdfjs-dist" :as pdfjs]
            [goog.object :as obj]
            [parzival.pdf]
            [re-frame.core :refer [dispatch]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def main-content-style
  {:grid-area "main-content"
   :display "flex"
   :flex "1 1 100%"
   :justify-content "stretch"
   :align-items "flex-start"
   :overflow-y "auto"
   :padding-top "2.5rem" ; TODO: Change the padding stuff
   :padding-left "2.5rem"
   :padding-right "2.5rem"
   :margin "40px"})

(def pdf-container-style
  {:height "500px"
   :width "300px"
   :border "1px solid blue"})

;;; Components

; (defn main-content
;   []
;   (let [worker (js/Worker. "/js/compiled/worker.js")
;         url    "http://proceedings.mlr.press/v70/shi17a/shi17a.pdf"
;         pdf    (atom nil)]
;     (obj/set (obj/getValueByKeys pdfjs "GlobalWorkerOptions") "workerSrc" "/js/compiled/worker.js")
;     (println (obj/getValueByKeys pdfjs "GlobalWorkerOptions" "workerSrc"))
;     ; (println (pdfjs/getDocument url))
;     ; (-> (pdfjs/getDocument url)
;     ;     (.then #(reset! pdf %)))
;     (println pdfjs)
;     (-> (js/Promise. (pdfjs/getDocument url))
;         (.then #(reset! pdf %)))
;     ; (pdfjs/getDocument url)
;     ; (println pdfjs/getDocument)
;     ; (-> (pdfjs/getDocument url)
;     ;     (.then #(reset! pdf %)))
;     ; (pdfjs/getDocument url)
;     ; (-> (pdfjs/getDocument url)
;     ;     (.then #(println %)));#(reset! pdf %))
;     (fn []
;       [:div (use-style main-content-style)
;        [:div (use-style pdf-container-style)]])))
                                             
(defn main-content
  []
    (fn []
      (obj/set (obj/getValueByKeys pdfjs "GlobalWorkerOptions") "workerSrc" "/js/compiled/pdf.worker.js")
      (js/console.log pdfjs)
      (pdfjs/getDocument "https://arxiv.org/pdf/2006.06676v2.pdf")
      ; (js/console.log (pdfjs/getDocument url))
       [:div (use-style main-content-style)
        [:div (use-style pdf-container-style)]]))
      
; (defn main-content
;   []
;   (fn []
;     (obj/set (obj/getValueByKeys pdfjs "GlobalWorkerOptions") "workerSrc" "/js/compiled/pdf-worker.js")
;     (js/console.log pdfjs)
;     (js/console.log (pdfjs/getDocument))
;     ; (dispatch [:pdf/load])
;      [:div (use-style main-content-style)
;       [:div (use-style pdf-container-style)]]))

