(ns parzival.subs
  (:require
   [re-frame.core :as re-frame]))


(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :left-sidebar/open
 (fn [db _]
   (:left-sidebar/open db)))

(re-frame/reg-sub
 :right-sidebar/open
 (fn [db _]
   (:right-sidebar/open db)))

(re-frame/reg-sub
 :right-sidebar/width
 (fn [db _]
   (:right-sidebar/width db)))

(re-frame/reg-sub
 :settings/open
 (fn [db _]
   (:settings/open db)))

(re-frame/reg-sub
 :search/open
 (fn [db _]
   (:search/open db)))

(re-frame/reg-sub
 :theme/dark
 (fn [db _]
   (:theme/dark db)))

(re-frame/reg-sub
  :pdf/document
  (fn [db _]
    (:pdf/document db)))

(re-frame/reg-sub
  :pdf/page
  (fn [db _]
    (:pdf/page db)))

(re-frame/reg-sub
  :loading/progress
  (fn [db _]
    (:loading/progress db)))

(re-frame/reg-sub
  :pdf?
  (fn [db _]
    (some? (get db :pdf))))

(re-frame/reg-sub
  :current-route
  (fn [db _]
    (get db :current-route)))