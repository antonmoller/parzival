(ns parzival.views.document-table
  (:require
   [parzival.style :refer [color OPACITIES]]
   [parzival.utils :refer [date-string]]
   [re-frame.core :refer [subscribe dispatch]]
   [clojure.string :refer [join]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

(def table-style
  {:flex "1 1 100%"
   :margin "5rem 2rem 0rem"
   :max-width "70rem"
   :text-align "left"
   :border-collapse "collapse"
   :overflow "auto"
   ::stylefy/sub-styles {:th-title {:cursor "pointer"}
                         :th-date {:text-align "right"
                                   :cursor "pointer"}
                         :td-title {:color (color :link-color)
                                    :width "25vw"
                                    :min-width "10em"
                                    :cursor "pointer"
                                    :word-break "break-word"
                                    :font-weight "500"
                                    :font-size "1.3125em"
                                    :line-height "1.28"}
                         :td-date {:text-align "right"
                                   :opacity (:opacity-high OPACITIES)
                                   :font-size "0.75em"
                                   :min-width "9em"
                                   :width "9em"}}
   ::stylefy/manual [[:tbody
                      {:vertical-align "top"}
                      [:tr {:transition "background 0.1s ease"}
                       [:td {:border-top (str "1px solid " (color :border-color :opacity-low))}]
                       [:&:hover {:background-color (color :background-plus-1-color :opacity-lower)}]]]
                     [:td :th {:padding "0.5rem"}]
                     [:th [:h5 {:opacity (:opacity-med OPACITIES)}
                           [:&:hover {:color (color :background-color)
                                      :opacity 1}]]]]})

(defn document-table
  [display?]
  (let [pages @(subscribe [:pages])]
    [:table (merge (use-style table-style)
                   (if display? {} {:style {:display "none"}}))
     [:thead
      [:tr
       [:th (use-sub-style table-style :th-title) [:h5 "TITLE"]]
       [:th (use-sub-style table-style :th-title) [:h5 "AUTHORS"]]
       [:th (use-sub-style table-style :th-date) [:h5 "MODIFIED"]]
       [:th (use-sub-style table-style :th-date) [:h5 "ADDED"]]]]
     [:tbody
      (doall
       (for [[uid {:keys [title authors filename modified added]}] pages]
         [:tr
          {:key uid}
          [:td (merge (use-sub-style table-style :td-title)
                      {:on-click (fn []
                                   (dispatch [:navigate :pdf])
                                   (dispatch [:pdf/load uid filename]))})
           title]
          [:td authors ;(join ", " authors)
           ]
          [:td (use-sub-style table-style :td-date) (date-string modified)]
          [:td (use-sub-style table-style :td-date) (date-string added)]]))]]))