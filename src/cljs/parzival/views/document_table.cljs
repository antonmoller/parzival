(ns parzival.views.document-table
  (:require
   [stylefy.core :as stylefy :refer [use-style]]))

(def table-style
  {:flex "1 1 100%"
   :max-width "70rem"
   :text-align "left"
   :border-collapse "collapse"})

(defn document-table
  []
  [:table (use-style table-style)
   [:thead
    [:tr
     [:th [:h5 "TITLE"]]
     [:th [:h5 "LINKS"]]
     [:th [:h5 "UPDATED"]]
     [:th [:h5 "ADDED"]]]]
   [:tbody
    [:tr
     [:td "testing 0"]
     [:td "testing 0"]
     [:td "testing 0"]
     [:td "testing 0"]]]])