(ns parzival.views.app-toolbar
  (:require
    ["@material-ui/icons" :as mui-icons]
    [re-frame.core :refer [subscribe dispatch]]
    [parzival.views.buttons :refer [button]]
    [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def app-toolbar-style
  {:grid-area "app-header"
   :display "flex"
   :flex-direction "row"
   :justify-content "space-between"
   :align-items "center"
   :position "absolute"
   :right 0
   :left 0
   :box-sizing "border-box"
   :border-bottom "2px solid red"
   :padding "0.25rem 0.75rem"})

(def app-toolbar-left-style
  {:grid-area "app-header 1"
   :display "flex"
   :flex-direction "row"
   :position "absolute"
   :background-clip "padding-box"
   ; :border-bottom "2px solid red"
   :padding "0.25rem 0.75rem"
   ::stylefy/manual [[:svg {:font-size "20px"}]
                     [:button {:justify-self "flex-start"}]]})

;;; Components

(defn app-toolbar
  []
  (let [left-open? (subscribe [:left-sidebar/open])
        right-open? (subscribe [:right-sidebar/open])]
    [:<>
     [:header (use-style app-toolbar-left-style)
      [button {:on-click #(dispatch [:left-sidebar/toggle])}
       [:> mui-icons/Menu]]]]))

   


    ; [:header (use-style app-toolbar-style)
    ;  [:div
    ;   [button {:on-click #(dispatch [:left-sidebar/toggle])}
    ;    [:> mui-icons/Menu]]]
    ;  [:div
    ;   [button {:on-click #(dispatch [:search/toggle])}
    ;    [:> mui-icons/Search]]
    ;   [button {:on-click #(dispatch [:settings/toggle])} 
    ;    [:> mui-icons/Settings]] 
    ;   [button {:on-click #(dispatch [:right-sidebar/toggle])} 
    ;    [:> mui-icons/VerticalSplit]]]]))
