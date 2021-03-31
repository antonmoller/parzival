(ns parzival.views.document-table
  (:require
   [parzival.style :refer [color OPACITIES]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

(def table-style
  {:flex "1 1 100%"
   :margin-top "7.5rem"
   :max-width "70rem"
   :text-align "left"
   :border-collapse "collapse"
   ::stylefy/sub-styles {:th-date {:text-align "right"}
                         :td-title {:color (color :link-color)
                                    :width "15vw"
                                    :cursor "pointer"
                                    :min-width "5em"
                                    :word-break "break-word"
                                    :font-weight "500"
                                    :font-size "1.3125em"
                                    :line-height "1.28"}
                         :td-tags {:font-size "1em"
                                   :min-widhth "5em"}
                         :td-date {:text-align "right"
                                   :opacity (:opacity-high OPACITIES)
                                   :font-size "0.75em"
                                   :min-width "9em"}}
   ::stylefy/manual [[:tbody
                      [:tr {:transition "background 0.1s ease"}
                        [:td {:border-top (str "1px solid " (color :border-color))}]
                        [:&:hover {:background-color (color :background-plus-1-color :opacity-med)}]]]
                     [:td :th {:padding "0.5rem"}]
                     [:th [:h5 {:opacity (:opacity-med OPACITIES)}]]]})

(defn document-table
  []
  [:table (use-style table-style)
   [:thead
    [:tr
     [:th [:h5 "TITLE"]]
     [:th [:h5 "TAGS"]]
     [:th (use-sub-style table-style :th-date) [:h5 "UPDATED"]]
     [:th (use-sub-style table-style :th-date) [:h5 "ADDED"]]]]
   [:tbody
    [:tr
     [:td (use-sub-style table-style :td-title) "testing 0"]
     [:td (use-sub-style table-style :td-tags) "testing 0"]
     [:td (use-sub-style table-style :td-date) "testing 0"]
     [:td (use-sub-style table-style :td-date) "testing 0"]]
    [:tr
     [:td (use-sub-style table-style :td-title) "testing 0"]
     [:td (use-sub-style table-style :td-tags) "testing 0"]
     [:td (use-sub-style table-style :td-date) "testing 0"]
     [:td (use-sub-style table-style :td-date) "testing 0"]]]])