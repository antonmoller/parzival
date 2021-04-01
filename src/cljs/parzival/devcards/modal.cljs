(ns parzival.devcards.modal
  (:require
   [parzival.views.settings :refer [settings]]
   [parzival.views.buttons :refer [button]]
   [re-frame.core :refer [dispatch]]
   [devcards.core :refer [defcard-rg]]))

(defcard-rg Toggle
  [button {:primary true :on-click #(dispatch [:settings/toggle])} "Toggle Settings"])

(defcard-rg Settings
  [settings])
