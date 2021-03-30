(ns parzival.views.document-table)

(defn document-table
  []
  [:table
   [:thead
    [:tr
     [:th [:h5 "TITLE"]]
     [:th [:h5 "LINKS"]]
     [:th [:h5 "UPDATED"]]
     [:th [:h5 "ADDED"]]]]])