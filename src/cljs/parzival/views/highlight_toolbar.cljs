(ns parzival.views.highlight-toolbar
  (:require 
   [re-frame.core :refer [dispatch subscribe]]
   ["@material-ui/icons/HighlightOff" :default HighlightOff]
   [stylefy.core :as stylefy :refer [use-style]]
   [parzival.style :refer [HIGHLIGHT-COLOR DEPTH-SHADOWS ZINDICES color]]))

;;; Styles

(def tooltip-style
  {:visibility "hidden"
   :width "162px"
   :background-color (color :background-plus-1-color)
   :box-shadow    (str (:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower))
   :color (color :body-text-color)
   :text-align "center"
   :border-radius "0.25rem"
   :position "absolute"
   :z-index (:zindex-tooltip ZINDICES)
   ::stylefy/mode {:after {:content "''"
                           :position "absolute"
                           :bottom "100%"
                           :left "50%"
                           :margin-left "-5px"
                           :border-width "5px"
                           :border-style "solid"
                           :border-color (str "transparent transparent " (color :background-plus-1-color) " transparent")}}
   ::stylefy/manual [[:&.is-visible {:visibility "visible"}]
                     [:&.is-hidden {:visibility "hidden"}]]})

(def highlight-style
  {:display "flex"
   :margin "0.25rem 0.5rem"
   :align-items "center"})

(def circle-style
  {:height "20px"
   :width "20px"
   :border-radius "50%"
   :line-height "1"
   :margin-right "0.25rem"
   :cursor "pointer"
   ::stylefy/manual {:svg {:font-size "20px"}}})

;;; Components

(defn highlight-toolbar
  []
  (let [highlight? (subscribe [:highlight/open])
        position   (subscribe [:highlight/anchor])]
    (fn []
      [:div (merge (use-style tooltip-style
                              {:class (if @highlight? "is-visible" "is-hidden")})
                   {:style {:left (str (- (first @position) 81) "px")
                            :top  (str (second @position) "px")}})
       [:div (use-style highlight-style)
        (doall
         (for [[_ color] HIGHLIGHT-COLOR]
           [:span (merge (use-style circle-style)
                         {:style {:background-color color}
                          :key (str "highlight-" color)
                          :on-click #(dispatch [:highlight color])})]))
        [:> HighlightOff {:style {:cursor "pointer"}
                          :on-click #(dispatch [:highlight/toggle])}]]])))