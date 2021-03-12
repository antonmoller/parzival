(ns parzival.views.left-sidebar
  (:require
            [re-frame.core :refer [subscribe]]
            ["@material-ui/icons" :as mui-icons]
            [parzival.views.buttons :refer [button]]
            [parzival.style :refer [color OPACITIES ZINDICES]]
            [reagent.core :as r]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

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
   :padding "2.5rem 1.25rem 5rem"})

(def menu-item-style
  {:display "flex"
   :flex "0 0 auto"
   :font-size "18px"
   :font-weight "bold"
   :cursor "pointer"
   :padding "0.25rem 0"
   ::stylefy/mode [[:hover {:background (color :body-text-color :opacity-lower)}]]})

(def icon-style
  {:padding-right "8px"})

(def divider-style
  {:border-bottom-style "solid"
   :border-bottom-width "1px"
   :padding-top "1rem"
   :border-bottom-color (color :body-text-color :opacity-med)})

(def shortcut-list-style
  {:flex "1 1 100%"
   :display "flex"
   :flex-direction "column"
   :padding-left 0
   :height "fixed"
   :overflow-y "auto"
   :list-style "none"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode [["::-webkit-scrollbar" {:width "0px"
                                           :background "transparent"}]]
   ::stylefy/sub-styles {:heading {:cursor "default"
                                   :flex "0 0 auto"
                                   :opacity (:opacity-med OPACITIES)
                                   :line-height "1"
                                   :margin "0 0 0.25rem"
                                   :font-size "16px"}}})

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
  (let [open? (subscribe [:left-sidebar/open])]
        [:div (use-style left-sidebar-style {:class (if @open? "is-open" "is-closed")})
         [:div (use-style left-sidebar-content-style)
          [:div
           [:div (use-style menu-item-style)
            [:> mui-icons/AccountBox (use-style icon-style)] 
            [:span "ACCOUNT"]
            [:> mui-icons/ExpandMore]]
           [:div (use-style menu-item-style)
            [:> mui-icons/Reorder (use-style icon-style)] 
            [:span "ALL DOCUMENTS"]]
           [:div (use-style menu-item-style)
            [:> mui-icons/Subtitles (use-style icon-style)] 
            [:span "ALL FLASHCARDS"]]
           [:div (use-style divider-style)]]
           [:ol (use-style shortcut-list-style)
             [:h2 (use-sub-style shortcut-list-style :heading) 
              [:> mui-icons/Star {:style {:font-size "16px" :padding-right "0.5rem"}}]
              [:span "SHORTCUTS"]]
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
            [button {:primary true} [:<> [:span "Start Learning"] [:> mui-icons/ChevronRight]]]
            [button [:<> [:span "Start Reviewing"] [:> mui-icons/ChevronRight]]]]]]))
