(ns parzival.views.settings
  (:require
            [parzival.views.modal :refer [modal]]
            [re-frame.core :refer [subscribe]]
            [stylefy.core :as stylefy :refer [use-style]]))

; (defn settings
;   []
;   [:h1 "testing"])

(defn settings
  []
  (let [open? (subscribe [:settings/open])]
   (fn []
    (when @open?
     [modal :settings/toggle]))))

