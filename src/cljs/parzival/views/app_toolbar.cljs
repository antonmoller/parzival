(ns parzival.views.app-toolbar
  (:require
    ["@material-ui/icons" :as mui-icons]
    [re-frame.core :refer [subscribe dispatch]]
    [parzival.style :refer [ZINDICES color]]
    [parzival.views.buttons :refer [button]]
    [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def toolbar-style
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   ; :position "fixed"
   :position "absolute"
   ; :top 0
   :z-index (:zindex-dropdown ZINDICES)
   :padding "0.25rem 0.75rem"
   ::stylefy/manual [[:svg {:font-size "20px"}]]})

(def left-toolbar-style
  (merge toolbar-style
         {:grid-area "left-header"
          :justify-content "flex-start"
          :left 0}))

(def right-toolbar-style
  (merge toolbar-style
         {:grid-area "right-header"
          :justify-content "flex-end"
          :right 0}))

;;; Components

(defn app-toolbar
  []
  (let [left-open? (subscribe [:left-sidebar/open])
        right-open? (subscribe [:right-sidebar/open])]
    [:<>
      [:header (use-style left-toolbar-style)
        [button {:on-click #(dispatch [:left-sidebar/toggle])}
         [:> mui-icons/Menu]]]
      [:header (use-style right-toolbar-style)
       [button {:on-click #(dispatch [:search/toggle])} 
        [:> mui-icons/Search]]
       [button {:on-click #((dispatch [:settings/toggle])
                            (dispatch [:navigate :settings]))} 
        [:> mui-icons/Settings]] 
       [button {:on-click #(dispatch [:right-sidebar/toggle])} 
        [:> mui-icons/VerticalSplit]]]]))
