(ns parzival.views.tooltip
  (:require
   [parzival.style :refer [color ZINDICES DEPTH-SHADOWS]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]
   ))

;;; Styles

(def tooltip-style
  {:position "relative"
   :display "inline-block"
   ::stylefy/sub-styles {:text {:visibility "hidden"
                                :width "auto"
                                :max-width "500px"
                                :background-color (color :background-plus-1-color)
                                :box-shadow    (str (:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower))
                                :color (color :body-text-color)
                                :text-align "center"
                                :border-radius "0.25rem"
                                :padding "5px 0"
                                :position "absolute"
                                :z-index (:zindex-tooltip ZINDICES)
                                :top "100%"
                                :left "50%"
                                :margin-left "-80px"
                                ::stylefy/mode {:after {:content "''"
                                                        :position "absolute"
                                                        :bottom "100%"
                                                        :left "50%"
                                                        :margin-left "-5px"
                                                        :border-width "5px"
                                                        :border-style "solid"
                                                        :border-color (str "transparent transparent " (color :background-plus-1-color) " transparent")
                                                        }}}}
   ::stylefy/manual [[:&:hover [:.text {:visibility "visible"}]]]})

;;; Components

(defn tooltip
  [container tooltip]
  [:div (use-style tooltip-style)
   container
   [:span.text (use-sub-style tooltip-style :text) 
    tooltip]])