(ns parzival.views.modal.modal
  (:require
   [parzival.views.modal.search :refer [search]]
   [parzival.views.modal.filesystem :refer [filesystem]]
   [parzival.views.modal.settings :refer [settings]]
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
  []
  (let [content @(subscribe [:modal/content])]
    (when (some? content)
      (dispatch [:modal/handle-click])
      [:div (use-style modal-style {:id "modal"})
       [(case content
          :search search
          :filesystem filesystem
          :settings settings)]])))

