(ns parzival.views.settings
  (:require
            [parzival.views.modal :refer [modal]]
            [parzival.style :refer [color]]
            ["@material-ui/icons" :as mui-icons]
            [re-frame.core :refer [subscribe]]
            [stylefy.core :as stylefy :refer [use-style]]))

(def wrapper
  {:display "flex"
   :flex-direction "row"
   :box-sizing "border-box"
   :width "400px"
   :height "200px"})

(def container-1-style
  {:display "flex"
   :flex-direction "column"
   :padding-top "2.5rem"
   :padding-bottom "2.5rem"
   :width "10rem"
   :height "200px"
   :border "1px solid red"
   :background-color (color :left-sidebar-color)})

(def menu-item-style
  {:display "flex"
   ; :flex "0 0 auto"
   ; :font-size "16px"
   ; :font-weight "bold"
   :cursor "pointer"
   :padding-top "0.25rem"
   :padding-left "0.5rem"
   ::stylefy/mode [[:hover {:background (color :body-text-color :opacity-lower)}]]})
   

(defn settings
  []
  (let [open? (subscribe [:settings/open])]
   (fn []
    (when @open?
     [modal :settings/toggle [:div (use-style wrapper)
                              [:div (use-style container-1-style)
                               [:div (use-style menu-item-style)
                                [:span "ACCOUNT"]]]
                              [:div (use-style container-1-style)]]]))))
