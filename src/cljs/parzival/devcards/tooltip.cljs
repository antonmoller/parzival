(ns parzival.devcards.tooltip
  (:require
   [parzival.views.tooltip :refer [tooltip]]
   [parzival.views.pdf.highlight-toolbar :refer [highlight-toolbar]]
   [devcards.core :refer [defcard-rg]]))


(defcard-rg Tooltip
  [:div {:style {:padding-left "100px" :height "100px"}}
    [tooltip
     [:div "Hover me"]
     [:span "Hahaha"]]
   ])

(defcard-rg Highlight-Toolbar
  [:div {:style {:padding-left "100px" :width "500px" :height "300px"}}
   [tooltip
    [:div "COLOURS"]
    [highlight-toolbar]]])