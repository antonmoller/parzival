(ns parzival.views.left-sidebar
  (:require
            [re-frame.core :refer [subscribe]]
            ["@material-ui/icons" :as mui-icons]
            [parzival.style :refer [color OPACITIES]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def left-sidebar-style
  {:grid-area "left-sidebar"
   :place-self "stretch stretch"
   :overflow-x "hidden"
   :overflow-y "auto"
   :background-color (color :left-sidebar-color)
   :transition "all 0.2s ease" ; try 0.3s wtf???
   ::stylefy/manual [[:&.is-open {:width "16rem"}]
                     [:&.is-closed {:width "0"}]]})

(def left-sidebar-content-style
  {:display "flex"
   :flex-direction "column"
   :height "100%"
   :width "16rem"
   :box-sizing "border-box"
   :padding "2.5rem 1.25rem"})

(def menu-item-style
  {:display "flex"
   :flex "0 0 auto"
   :font-size "18px"
   :font-weight "bold"
   :cursor "pointer"
   :padding "0.25rem 0"
   ::stylefy/mode [[:hover {:background (color :body-text-color :opacity-lower)}]]})

(def icon-style
  {:padding-right "0.25rem"})

(def divider-style
  {:border-bottom-style "solid"
   :border-bottom-width "1px"
   :padding-top "1rem"
   :border-bottom-color (color :body-text-color :opacity-med)})

(defn menu
  []
  [:<>
    [:div (use-style menu-item-style)
     [:> mui-icons/AccountBox (use-style icon-style)] 
     "ACCOUNT"
     [:> mui-icons/ExpandMore]]
    [:div (use-style menu-item-style)
     [:> mui-icons/Reorder (use-style icon-style)] 
     "ALL DOCUMENTS"]
    [:div (use-style menu-item-style)
     [:> mui-icons/Subtitles (use-style icon-style)] 
     "ALL FLASHCARDS"]
    [:div (use-style divider-style)]])

  
(defn left-sidebar
  []
  (let [open? (subscribe [:left-sidebar/open])]
        [:div (use-style left-sidebar-style {:class (if @open? "is-open" "is-closed")})
         [:div (use-style left-sidebar-content-style)
          [menu]
         ]]))







          ; [:div (use-style menu-style)
          ;    [:h3 (use-style menu-item-style) 
          ;     [:> mui-icons/Reorder] " ALL DOCUMENTS"]
          ;   [:a "SPACED REPETITION"]]
          ; [:div (use-style divider-style)]
          ; [:div "testing this out"]
          ; [:span "testing this very long even longer link out"]
          ; [:h3 "testing this out"]
          ; [:h3 "testing this out"]
          ; [:h3 "testing this out"]
