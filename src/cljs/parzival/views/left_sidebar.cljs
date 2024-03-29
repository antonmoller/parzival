(ns parzival.views.left-sidebar
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   ["@material-ui/icons/Star" :default Star]
   ["@material-ui/icons/Add" :default Add]
   ["@material-ui/icons/ChevronRight" :default ChevronRight]
   [parzival.utils :as utils]
   [parzival.views.buttons :refer [button]]
   [parzival.style :refer [color OPACITIES]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles
;TODO
(def left-sidebar-style
  {:grid-area "left-sidebar"
   :user-select "none"
  ; :position "fixed"
   :width 0
   :overflow-x "hidden"
   :overflow-y "hidden"
   :height "100%"
   :display "flex"
   :flex-direction "column"
   :background-color (color :left-sidebar-color)
   :background-clip "padding-box"
   :transition "all 0.3s ease" 
   ::stylefy/manual [[:&.is-open {:width "16rem"}]
                     [:&.is-closed {:width "0"}]]})

(def left-sidebar-content-style
  {:display "flex"
   :flex-direction "column"
   :height "100%"
   :width "16rem"
   :box-sizing "border-box"
   :padding "7.5rem 1.25rem 2.5rem"})

(def shortcut-list-style
  {:flex "1 1 100%"
   :display "flex"
   :flex-direction "column"
   :padding-left 0
   :height "fixed"
   :overflow-y "auto"
   :list-style "none"
   :scrollbar-width "none"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode [["::-webkit-scrollbar" {:display "none"}]]})

(def headline-style
{:cursor "default"
 :flex "0 0 auto"
 :opacity (:opacity-med OPACITIES)
 :line-height "1"
 :font-size "16px"
 ::stylefy/manual [[:svg {:font-size "16px"
                          :margin-right "0.5rem"}]]})

(def shortcut-style
  {:color (color :body-text-color)
   :cursor "pointer"
   :display "flex"
   :flex "0 0 auto"
   :padding "0.25rem 0"
   ::stylefy/mode [[:hover {:background (color :body-text-color :opacity-lower)}]]})

(def button-container-style
  {:display "grid"
   :grid-template-rows "auto auto"
   :grid-auto-flow "row"
   :height "auto"
   :row-gap "1rem"})

(defn left-sidebar
  []
  (let [open? (subscribe [:left-sidebar/open?])]
    [:div (use-style left-sidebar-style {:class (if @open? "is-open" "is-closed")})
     [:div (use-style left-sidebar-content-style)
      [:h2 (use-style headline-style)
       [:> Star]
       [:span "SHORTCUTS"]]
      [:ol (use-style shortcut-list-style)
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]
       [:li [:div (use-style shortcut-style) [:span "testing"]]]]
      [:div (use-style button-container-style)
       [button {:primary true} [:<> [:span "Start Learning"] [:> ChevronRight]]]
       [button [:<> [:span "Start Reviewing"] [:> ChevronRight]]]
       [button {:on-click #(dispatch [:modal/set-content :filesystem])
                :disabled (not (utils/electron?))}
        [:<> [:span "Upload Files"] [:> Add]]]]]]))
