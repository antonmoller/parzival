(ns parzival.views.app-toolbar
  (:require
   ["@material-ui/icons/Menu" :default Menu]
   ["@material-ui/icons/FileCopy" :default FileCopy]
   ["@material-ui/icons/FiberManualRecord" :default FiberManualRecord]
   ["@material-ui/icons/InsertDriveFile" :default InsertDriveFile]
   ["@material-ui/icons/Search" :default Search]
   ["@material-ui/icons/StarBorder" :default StarBorder]
   ["@material-ui/icons/Settings" :default Settings]
   ["@material-ui/icons/VerticalSplit" :default VerticalSplit]
   [re-frame.core :refer [subscribe dispatch]]
   [parzival.utils :as utils]
   [parzival.style :refer [ZINDICES]]
   [parzival.views.buttons :refer [button]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def toolbar-style
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :position "absolute"
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
   :padding-top "0.375rem"})

;;; Components

(defn app-toolbar
  []
  (let [left-open?     (subscribe [:left-sidebar/open?])
        right-open?    (subscribe [:right-sidebar/open?])
        synced?        (subscribe [:db/synced?])
        modal-content  (subscribe [:modal/content])
        route-name     (subscribe [:current-route/name])]
    [:<>
     [:header (use-style left-toolbar-style)
      [button {:on-click #(dispatch [:left-sidebar/toggle])
               :active @left-open?}
       [:> Menu]]
      [button {:on-click #(dispatch [:navigate :documents])
               :active (= @route-name :documents)}
       [:> FileCopy]]
      [button {:on-click #(dispatch [:navigate :pdf])
               :active (= @route-name :pdf)}
       [:> InsertDriveFile]]]
     [:header (use-style right-toolbar-style)
      (when (utils/electron?)
        [:div (use-style save-state-style)
         [:> FiberManualRecord {:style {:color (if @synced? "green" "yellow")}}]])
      [button {:on-click #(dispatch [:modal/set-content :search])
               :active (= :search @modal-content)}
       [:> Search]]
      [button
       [:> StarBorder]]
      [button {:on-click #(dispatch [:modal/set-content :settings])
               :active (= :setting @modal-content)}
       [:> Settings]]
      [button {:on-click #(dispatch [:right-sidebar/toggle])
               :active @right-open?}
       [:> VerticalSplit]]]]))
