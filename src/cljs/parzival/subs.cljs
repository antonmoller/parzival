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
