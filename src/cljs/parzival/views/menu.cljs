(ns parzival.views.menu
  (:require
   [parzival.style :refer [color OPACITIES ZINDICES DEPTH-SHADOWS]]
   [re-frame.core :refer [subscribe]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def menu-style
  {:z-index {:zindex-modal ZINDICES}
   :background-color (color :background-plus-1-color)
   :box-shadow    (str (:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower))
   :border-radius "0.25rem"
   :list-style-type "none"
   :width "125px"
   :padding "1rem 0"
   :text-align "center"})

(def item-style
  {:cursor "pointer"
   :font-size "16px"
   :font-weight 500
   :color (color :body-text-color)
   ::stylefy/mode {:hover {:background-color (color :body-text-color :opacity-low)}}})

;;; Components

(defn menu
  []
   [:ul (use-style menu-style)
    [:li (use-style item-style) "Pagemark"]
    [:li (use-style item-style) "Pagemark"]
    [:li (use-style item-style) "Pagemark"]
    [:li (use-style item-style) "Pagemark"]
    [:li (use-style item-style) "Pagemark"]])