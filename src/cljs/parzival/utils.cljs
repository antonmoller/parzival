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

(defn dec-to-percentage
  [dec]
  (-> dec (* 100) (str "%")))

(defn px-to-percentage
  [bounding-px new-px]
  (-> new-px
      (/ bounding-px)
      (* 100)
      (str "%")))

(defn percentage-to-float
  [p]
  (-> (js/parseFloat p)
      (/ 100)))

(defn height-percentage
  [{:keys [start-page end-page end-area]} page-quota]
  (-> end-page (+ end-area) (- start-page) (* page-quota 100) (str "%")))

(defn top-percentage
  [{:keys [start-page]} page-quota]
  (-> start-page (dec) (* page-quota 100) (str "%")))

; TODO move to pdf
(defn pdf-page-num
  [target]
  (-> (.closest target ".page")
      (.getAttribute "data-page-number")
      (int)))

; TODO move to pdf, no just rename to get-uid
(defn highlight-uid
  [highlight]
  (.getAttribute highlight "id"))

;; (defn check-spec
;;   "Throws and exception if 'value' doesn't match 'spec'"
;;   [spec value]
;;   (when-not (s/valid? spec value)
;;     (throw (ex-info (str "spec-check-failed: " (s/explain-str spec value)) {}))))

(defn check-spec
  "Throws and exception if 'value' doesn't match 'spec'"
  [spec value]
  (if (s/valid? spec value)
    value
    (throw (ex-info (str "spec-check-failed: " (s/explain-str spec value)) {}))))

(def check-db (after (fn [db]
                       (when-not (s/valid? :parzival.db/db db)
                         (throw (ex-info (str "spec-check-failed: "
                                              (s/explain-str :parzival.db/db db)) {}))))))

(defn electron?
  []
  (->> (.. js/navigator -userAgent toLowerCase) (re-find #"electron") (boolean)))