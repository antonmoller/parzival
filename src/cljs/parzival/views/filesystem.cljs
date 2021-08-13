(ns parzival.views.filesystem
  (:require
   [parzival.views.modal :refer [modal]]
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
  [modal
   {:id "filesystem-modal"
    :open? :fs/open?
    :toggle :fs/toggle
    :content [:div (use-style filesystem-style)
              [:div (use-style drop-style)
               [:h4  "Drag and Drop " [:kbd "PDF Files"] " here to add"]
               [:> Save {:style {:font-size "5em"}}]]
              [button {:on-click (fn [_]
                                   (dispatch [:fs/pdf-dialog])
                                  ;;  (.click (.getElementById js/document "file-upload"))
                                   )
                       :primary "true"
                       :style {:width "7em"}}
               [:span "Browse Files"]]]}])