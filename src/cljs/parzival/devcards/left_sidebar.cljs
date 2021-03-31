(ns parzival.devcards.left-sidebar
  (:require
   [parzival.views.buttons :refer [button]]
   [parzival.views.left-sidebar :refer [left-sidebar]]
   [devcards.core :refer [defcard-rg]]
   [re-frame.core :refer [dispatch]]))


(defcard-rg Toggle
  [button {:primary true :on-click #(dispatch [:left-sidebar/toggle])} "Toggle"])

(defcard-rg Left-Sidebar
  [:div {:style {:display "flex" :height "60vh"}}
   [left-sidebar]]
  {:padding false})