(ns parzival.devcards.app-toolbar
  (:require
   [parzival.views.app-toolbar :refer [app-toolbar]]
   [devcards.core :refer [defcard-rg]]))

(defcard-rg App-Toolbar
  [:div {:style {:display "flex" :align-items "center"}}
   [app-toolbar]]
  {:padding false})