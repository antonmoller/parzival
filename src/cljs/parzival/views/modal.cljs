(ns parzival.views.modal
  (:require
            [re-frame.core :refer [dispatch]]
            [parzival.style :refer [color ZINDICES DEPTH-SHADOWS]]
            [stylefy.core :as stylefy :refer [use-style]]))

(def modal-style
  {:z-index (:zindex-modal ZINDICES)
   :position "fixed"
   :top "50vh"
   :left "50vw"
   :transform "translate(-50%, -50%)"
   :background-clip "padding-box"
   :border-radius "0.25rem"
   :background (color :background-1-color)
   :box-shadow    [[(:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :background-color :opacity-lower)]]
   :animation "fade-in 0.2s"})

(defn modal
  [toggle content]
  [:div (use-style modal-style {:on-click #(dispatch [toggle])})
    content])
