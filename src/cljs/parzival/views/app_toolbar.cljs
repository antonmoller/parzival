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
   :grid-gap "0.25rem"
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

(def save-state-style
  {:padding-right "0.625rem"
   :padding-top "0.375rem"
   :color "green"})

;;; Components

(defn app-toolbar
  []
  (let [left-open?  (subscribe [:left-sidebar/open])
        right-open? (subscribe [:right-sidebar/open])
        route-name  (subscribe [:current-route/name])]
        (js/console.log @route-name)
    [:<>
      [:header (use-style left-toolbar-style)
        [button {:on-click #(dispatch [:left-sidebar/toggle])
                 :active @left-open?}
         [:> mui-icons/Menu]]
        [button {:on-click #(dispatch [:navigate :documents])
                 :active (= @route-name :documents)}
          [:> mui-icons/FileCopy]]
        [button {:on-click #(dispatch [:navigate :pdf])
                 :active (= @route-name :pdf)}
         [:> mui-icons/InsertDriveFile]]]
        [:header (use-style right-toolbar-style)
       [:div (use-style save-state-style)
        [:> mui-icons/FiberManualRecord]]
       [button {:on-click #(dispatch [:search/toggle])} 
        [:> mui-icons/Search]]
       [button
        [:> mui-icons/StarBorder]]
       [button {:on-click #(dispatch [:settings/toggle])} 
        [:> mui-icons/Settings]] 
       [button {:on-click #(dispatch [:right-sidebar/toggle])
                :active @right-open?} 
        [:> mui-icons/VerticalSplit]]]]))
