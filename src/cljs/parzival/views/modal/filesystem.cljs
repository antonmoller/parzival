(ns parzival.views.modal.filesystem
  (:require
   [parzival.views.buttons :refer [button]]
   ["@material-ui/icons/Save" :default Save]
   [re-frame.core :refer [dispatch]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def filesystem-style
  {:display "flex"
   :flex-direction "column"
   :justify-content "space-between"
   :box-sizing "border-box"
   :padding "2rem"
   :width "27rem"})

(def drop-style
  {:display "flex"
   :flex-direction "column"
   :justify-content "space-between"
   :align-items "center"
   :height "8rem"})

;;; Components

(defn filesystem
  []
  [:div (use-style filesystem-style)
   [:div (use-style drop-style)
    [:h4  "Drag and Drop " [:kbd "PDF Files"] " here to add"]
    [:> Save {:style {:font-size "5em"}}]]
   [button {:on-click #(dispatch [:fs/pdf-add])
            :primary "true"
            :style {:width "7em"}}
    [:span "Browse Files"]]])