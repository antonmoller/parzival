(ns parzival.views.search
  (:require
            [parzival.views.modal :refer [modal]]
            [re-frame.core :refer [subscribe]]
            [stylefy.core :as stylefy :refer [use-style]]))

(defn search
  []
  (let [open? (subscribe [:search/open])]
    (fn []
      (when @open?
        [modal :search/toggle]))))
