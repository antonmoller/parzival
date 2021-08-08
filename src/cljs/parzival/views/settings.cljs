(ns parzival.views.settings
  (:require
            [parzival.views.modal :refer [modal]]
            [parzival.style :refer [color]]
            ["@material-ui/icons/AccountCircle" :default AccountCircle]
            ["@material-ui/icons/Brightness6" :default Brightness6]
            [re-frame.core :refer [subscribe]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def sidebar-style 
  {:width "12rem"
   :height "40rem"
   :padding "2.5rem 0rem 2.5rem 1rem"
   :background-color (color :left-sidebar-color)})

(def item-style
  {:cursor "pointer"
   :font-size "16px"
   :font-weight "500"
   :color (color :body-text-color)
   :display "flex"
   :flex "0 0 auto"
   :line-height "1"
   :padding "0.25rem 0"
   ::stylefy/manual [[:svg {:font-size "16px"
                            :margin-right "0.5rem"}]]
   ::stylefy/mode [[:hover {:background-color (color :body-text-color :opacity-lower)}]]})

(def content-style
 {:display "flex"
  :flex-direction "column"
  :padding "2.5rem"
  :width "30rem"
  :height "100%"})

;;; Components
   
(defn settings
  []
  (let [open? (subscribe [:settings/open])]
    (fn []
      (when @open?
        [modal :settings/toggle
         [:div {:style {:display "flex"}}
          [:div (use-style sidebar-style)
           [:div (use-style item-style)
            [:> AccountCircle]
            [:span "Account"]]
           [:div (use-style item-style)
            [:> Brightness6]
            [:span "Appearance"]]]
          [:div (use-style content-style)
           [:div "testing"]
           [:div "testing 2"]]]]))))