(ns parzival.devcards.table
  (:require
   [parzival.views.document-table :refer [document-table]]
   [devcards.core :refer [defcard-rg]]))

(defcard-rg Document-Table
[:div {:style {:display "flex" :justify-content "center"}}
  [document-table]]
  {:padding false})
