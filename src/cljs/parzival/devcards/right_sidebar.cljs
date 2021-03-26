(ns parzival.devcards.right-sidebar
  (:require
            [parzival.views.buttons :refer [button]]
            [parzival.views.right-sidebar :refer [right-sidebar]]
            [devcards.core :refer [defcard-rg]]
            [re-frame.core :refer [dispatch]]))

(defcard-rg Toggle
  [button {:primary true :on-click #(dispatch [:right-sidebar/toggle])} "Toggle"])

(defcard-rg Right-Sidebar
  [right-sidebar]
  {:padding false})




