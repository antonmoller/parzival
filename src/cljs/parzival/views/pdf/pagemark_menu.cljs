(ns parzival.views.pdf.pagemark-menu
  (:require
   ["@material-ui/icons/Book" :default Book]
   ["@material-ui/icons/Bookmark" :default Bookmark]
   ["@material-ui/icons/BookmarkBorder" :default BookmarkBorder]
   ["@material-ui/icons/Remove" :default Remove]
   [re-frame.core :refer [subscribe dispatch]]
   [parzival.style :refer [color ZINDICES DEPTH-SHADOWS]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

;;; Styles

(def menu-style
  {:z-index (:zindex-tooltip ZINDICES)
   :position "absolute"
   :visibility "hidden"
   :cursor "pointer"
   :left "40px"
   :top "40px"
   :border-radius "0.25rem"
   :padding 0
   :margin 0
   :background-color (color :background-plus-1-color)
   :box-shadow    (str (:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower))
   :list-style-type "none"
   ::stylefy/sub-styles {:item {:cursor "pointer"
                                :font-size "16px"
                                :font-weight 500
                                :padding "0.5rem 1rem"
                                :color (color :body-text-color)
                                ::stylefy/mode {:hover {:background-color (color :body-text-color :opacity-low)}}
                                ::stylefy/manual [[:svg {:font-size "16px"
                                                         :margin-right "0.5rem"}]]}}})

;;; Components

(defn pagemark-menu
  []
  (let [{:keys [left top height edit? page]} @(subscribe [:pagemark/anchor])]
    (when (some? left)
      (dispatch [:pagemark/close]))
    [:ul#pagemark-menu (merge (use-style menu-style)
                              {:style {:visibility (if (some? left) "visible" "hidden")
                                       :left left
                                       :top top}})
     [:li (merge (use-sub-style menu-style :item)
                 {:on-mouse-down #(dispatch [:pagemark/add page 1 height])})
      [:> BookmarkBorder]
      [:span "Pagemark to Current Location"]]
     [:li (merge (use-sub-style menu-style :item)
                 {:on-mouse-down #(dispatch [:pagemark/add page 1 1])})
      [:> Bookmark]
      [:span "Mark Entire Page as Read"]]
     [:li (merge (use-sub-style menu-style :item)
                 {:on-mouse-down #(dispatch [:pagemark?])})
      [:> Book]
      [:span "Skip/Schedule Pages"]]
     (when edit?
       [:li (merge (use-sub-style menu-style :item)
                   {:on-mouse-down #(dispatch [:pagemark/remove page])
                    :style {:color "red"}})
        [:> Remove]
        [:span "Remove Pagemark"]])]))