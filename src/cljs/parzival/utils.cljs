(ns parzival.utils
  (:require
   [re-frame.core :refer [after]] 
   [clojure.spec.alpha :as s]))

(defn date-string
  [timestamp]
  (-> timestamp  (js/Date.) (.toLocaleString "default" (js-obj "month" "long"
                                                               "day" "numeric"
                                                               "year" "numeric"
                                                               "hour" "numeric"
                                                               "minute" "numeric"))))

(defn gen-uid
  [prefix]
  (str prefix "-" (random-uuid)))

(defn check-spec
  "Throws and exception if 'value' doesn't match 'spec'"
  [spec value]
  (if (s/valid? spec value)
    value
    (throw (ex-info (str "spec-check-faild: " (s/explain-str spec value)) {}))))

(def check-db (after (partial check-spec :parzival.db/db)))