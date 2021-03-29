(ns parzival.router
  (:require
    [re-frame.core :as rf]
    [reitit.frontend :as rfe]
    [reitit.frontend.controllers :as rfc]
    [reitit.coercion.spec :as rss]
    [reitit.frontend.easy :as rfee]))

;;; Subs

(rf/reg-sub
  :current-route
  (fn [db]
    (-> db :current-route)))

;;; Events

(rf/reg-event-fx
  :navigate
  (fn [_ [_ & route]]
    {:navigate! route}))

(rf/reg-fx 
  :navigate!
  (fn [route]
    (apply rfee/push-state route)))

(rf/reg-event-db
  :navigated
  (fn [db [_ new-match]]
    (let [old-match (:current-route db)
          controllers (rfc/apply-controllers (:controllers old-match) new-match)]
      (assoc db :current-route (assoc new-match :controllers controllers)))))


;;; Routes

(def routes
  ["/"
   ["" {:name :home}]
   ["settings" {:name :settings}]])

(def router
  (rfe/router
    routes
    {:data {:coercion rss/coercion}}))

(defn on-navigate
  [new-match]
  (when new-match
    (rf/dispatch [:navigated new-match])))

(defn init-routes! 
  []
  (js/console.log "Initializing Routes")
  (rfee/start!
    router
    on-navigate
    {:use-fragment true}))


   
              


