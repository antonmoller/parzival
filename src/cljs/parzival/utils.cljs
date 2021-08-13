(ns parzival.utils)

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