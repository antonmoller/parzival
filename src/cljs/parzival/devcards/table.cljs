(ns parzival.devcards.table
  (:require
   [parzival.views.table :refer [table]]
   [parzival.views.document-table :refer [document-table]]
   [devcards.core :refer [defcard-rg]]))

(defcard-rg Document-Table
  [document-table]
  {:padding false})
