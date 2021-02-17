(ns parzival.views.modal
  (:require
            [re-frame.core :refer [dispatch]]
            [parzival.style :refer [color ZINDICES DEPTH-SHADOWS]]
            [stylefy.core :as stylefy :refer [use-style]]))

(def modal-wrapper-style
  {:z-index (:zindex-modal ZINDICES)
   :position "relative"
   :animation "fade-in 0.2s"})

(def backdrop-style
  {:z-index 1
   :position "fixed"
   :height "100vh"
   :width "100vw"
   :display "flex"
   :background "rgba(0, 0, 0, 0.2)"})

(def modal-style
  {:z-index 2
   :position "fixed"
   :top "50vh"
   :left "50vw"
   :transform "translate(-50%, -50%)"
   :background-clip "padding-box"
   :border-radius "0.25rem"
   :background (color :background-plus-1-color)
   :box-shadow    [[(:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower)]]})
   ; :box-shadow "0 3px 7px rgba(0, 0, 0, 0.3)"})

(defn modal
  [toggle content]
  [:div (use-style modal-wrapper-style)
    [:div (use-style backdrop-style {:on-click #(dispatch [toggle])})]
    [:div (use-style modal-style)
     content]
    ])
