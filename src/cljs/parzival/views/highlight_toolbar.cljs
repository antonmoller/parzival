(ns parzival.views.highlight-toolbar
  (:require 
   ["@material-ui/icons/HighlightOff" :default HighlightOff]
   [stylefy.core :as stylefy :refer [use-style]]
   [parzival.style :refer [HIGHLIGHT-COLOR]]))

(def highlight-style
  {:display "flex"
   :margin "0.25rem 0.5rem"
   :justify-content "center"
   :align-items "center"})

(def circle-style
  {:height "20px"
   :width "20px"
   :border-radius "50%"
   :line-height "1"
   :margin-right "0.25rem"
   ::stylefy/manual {:svg {:font-size "20px"}}})

(defn highlight-toolbar
  []
  [:div (use-style highlight-style)
   (doall
    (for [[_ color] HIGHLIGHT-COLOR]
      [:span (merge (use-style circle-style)
                    {:style {:background-color color}})]))
    [:> HighlightOff]])


