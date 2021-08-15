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
 :modal/content
 (fn [db _]
   (:modal/content db)))
   
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
 :documents
 (fn [db _]
   (:documents db)))

(reg-sub
 :db/synced?
 (fn [db _]
   (:db/synced? db)))

(reg-sub
 :db/sync-time
 (fn [db _]
   (:db/sync-time db)))