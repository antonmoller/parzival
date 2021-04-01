(ns parzival.views.modal
  (:require
            [re-frame.core :refer [dispatch]]
            [parzival.style :refer [color ZINDICES DEPTH-SHADOWS]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def modal-container-style
 {:z-index (:zindex-modal ZINDICES)
  :position "fixed"
  :inset "0px"
  :width "100vw"
  :height "100vh"
  :display "flex"
  :justify-content "center"
  :align-items "center"})

(def backdrop-style
  {:position "absolute"
   :inset "0px"
   :background-color (color :background-color :opacity-low)})

(def modal-style
{:position "relative"
 :z-index 1 
 :border-radius "0.25rem"
 :background-color (color :background-plus-1-color)
 :box-shadow    [[(:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower)]]})

;;; Components

(defn modal
  [toggle content]
  [:div (use-style modal-container-style)
   [:div (use-style backdrop-style {:on-click #(dispatch [toggle])})]
   [:div (use-style modal-style)
    content]])
