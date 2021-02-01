(ns parzival.views.app-toolbar
  (:require
    ["@material-ui/icons" :as mui-icons]
    [re-frame.core :refer [subscribe dispatch]]
    [parzival.style :refer [ZINDICES color]]
    [parzival.views.buttons :refer [button]]
    [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def app-toolbar-style
  {:grid-area "app-header"
   :display "flex"
   :flex-direction "row"
   :align-items "center"
   :justify-content "space-between"
   :position "absolute"
   :background-clip "padding-box"
   :left 0
   :right 0
   :z-index (:zindex-dropdown ZINDICES)
   :padding "0.25rem 0.75rem"
   ::stylefy/manual [[:svg {:font-size "20px"}]]})



;;; Components

(defn app-toolbar
  []
  (let [left-open? (subscribe [:left-sidebar/open])
        right-open? (subscribe [:right-sidebar/open])]
    [:header (use-style app-toolbar-style)
     [:div
      [button {:on-click #(dispatch [:left-sidebar/toggle])}
       [:> mui-icons/Menu]]]
     [:div
      [button {:on-click #(dispatch [:search/toggle])}
       [:> mui-icons/Search]]
      [button {:on-click #(dispatch [:settings/toggle])} 
       [:> mui-icons/Settings]] 
      [button {:on-click #(dispatch [:right-sidebar/toggle])} 
       [:> mui-icons/VerticalSplit]]]]))
