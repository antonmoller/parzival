(ns parzival.views.document-table
  (:require
   [parzival.style :refer [color OPACITIES]]
   [re-frame.core :refer [subscribe dispatch]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

(def table-style
  {:flex "1 1 100%"
   :margin "5rem 2rem 0rem"
   :max-width "70rem"
   :text-align "left"
   :border-collapse "collapse"
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
                         :td-tags {:text-align "center"
                                   :font-size "1em"}
                         :td-date {:text-align "right"
                                   :opacity (:opacity-high OPACITIES)
                                   :font-size "0.75em"
                                   :min-width "9em"}}
   ::stylefy/manual [
                     ;[:thead
                      ;; [:th [:&:hover {:color "green"}]]]
                     [:tbody
                      {:vertical-align "top"}
                      [:tr {:transition "background 0.1s ease"}
                       [:td {:border-top (str "1px solid " (color :border-color :opacity-low))}]
                       [:&:hover {:background-color (color :background-plus-1-color :opacity-lower)}]]]
                     [:td :th {:padding "0.5rem"}]
                     [:th [:h5 {:opacity (:opacity-med OPACITIES)}
                      [:&:hover {:color (color :background-color)
                                 :opacity 1}]
                           ]
                      ]]})

(defn document-table
  []
  (let [documents @(subscribe [:documents])]
    [:table (use-style table-style)
     [:thead
      [:tr
       [:th (use-sub-style table-style :th-title) [:h5 "TITLE"]]
       [:th (use-sub-style table-style :th-title) [:h5 "LINKS"]]
       [:th (use-sub-style table-style :th-date) [:h5 "MODIFIED"]]
       [:th (use-sub-style table-style :th-date) [:h5 "ADDED"]]]]
     [:tbody
      (doall
       (for [[uid {:keys [title filename]}] documents]
         [:tr
          {:key uid}
          [:td (merge (use-sub-style table-style :td-title)
                      {:on-click (fn []
                                   (dispatch [:pdf/set-nil])
                                   (dispatch [:pdf/active filename]))})
           title]
          [:td (use-sub-style table-style :td-tags) 42]
          [:td (use-sub-style table-style :td-date) "testing 0"]
          [:td (use-sub-style table-style :td-date) "testing 0"]]))]]))