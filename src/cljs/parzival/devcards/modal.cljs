(ns parzival.devcards.modal
  (:require
   [parzival.views.modal.settings :refer [settings]]
   [parzival.views.menu :refer [menu]]
   [parzival.views.modal.search :refer [search]]
   [parzival.views.modal.filesystem :refer [filesystem]]
   [parzival.views.buttons :refer [button]]
   [re-frame.core :refer [dispatch]]
   [devcards.core :refer [defcard-rg]]))

(defcard-rg Toggle
  [:div {:style {:display "flex" :justify-content "space-between"}}
   [button {:primary true :on-click #(dispatch [:settings/toggle])} "Toggle Settings"]
   [button {:primary true :on-click #(dispatch [:filesystem/toggle])} "Toggle Filesystem"]
   [button {:primary true :on-click #(dispatch [:search/toggle])} "Toggle Search"]])

(defcard-rg Filesystem
  [filesystem])

(defcard-rg Settings
  [settings])

(defcard-rg Search
  [search])

(defcard-rg Menu
  [menu])
