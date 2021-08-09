(ns parzival.subs
  (:require
   [re-frame.core :refer [reg-sub]]))


(reg-sub
 ::name
 (fn [db]
   (:name db)))

(reg-sub
 :left-sidebar/open
 (fn [db _]
   (:left-sidebar/open db)))

(reg-sub
 :right-sidebar/open
 (fn [db _]
   (:right-sidebar/open db)))

(reg-sub
 :right-sidebar/width
 (fn [db _]
   (:right-sidebar/width db)))

(reg-sub
 :settings/open
 (fn [db _]
   (:settings/open db)))

(reg-sub
 :theme/dark
 (fn [db _]
   (:theme/dark db)))

(reg-sub
  :loading/progress
  (fn [db _]
    (:loading/progress db)))

(reg-sub
  :current-route
  (fn [db _]
    (:current-route db)))

(reg-sub
 :search/open?
 (fn [db _]
   (:search/open? db)))

(reg-sub
 :filesystem/open?
 (fn [db _]
   (:filesystem/open? db)))