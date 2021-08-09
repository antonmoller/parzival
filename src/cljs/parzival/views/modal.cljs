(ns parzival.views.modal
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [parzival.style :refer [color ZINDICES DEPTH-SHADOWS]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def modal-style
  {:position "absolute"
   :z-index (:zindex-modal ZINDICES)
   :transform "translate(-50%, -50%)"
   :left "50%"
   :top "50%"
   :border-radius "0.25rem 0.25rem 0 0"
   :background-color (color :background-plus-1-color)
   :box-shadow [[(:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower)]]})

;;; Components

(defn modal
  [{:keys [id open? toggle content]}]
  (let [visible? @(subscribe [open?])]
    (when visible?
      (dispatch [:modal/handle-click id toggle])
      [:div (use-style modal-style {:id id})
       content])))

