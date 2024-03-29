(ns parzival.router
  (:require
    [re-frame.core :refer [reg-sub reg-event-fx reg-event-db reg-fx dispatch]]
    [reitit.frontend :as rfe]
    [reitit.frontend.controllers :as rfc]
    [reitit.coercion.spec :as rss]
    [reitit.frontend.easy :as rfee]))

;;; Subs

(reg-sub
  :current-route
  (fn [db]
    (-> db :current-route)))

(reg-sub
  :current-route/name
  (fn [db]
    (-> db :current-route :data :name)))

;;; Events

(reg-event-fx
  :navigate
  (fn [_ [_ & route]]
    {:navigate! route}))

(reg-fx 
  :navigate!
  (fn [route]
    (apply rfee/push-state route)))

(reg-event-db
  :navigated
  (fn [db [_ new-match]]
    (let [old-match (:current-route db)
          controllers (rfc/apply-controllers (:controllers old-match) new-match)]
      (assoc db :current-route (assoc new-match :controllers controllers)))))

;;; Routes

(def routes
  ["/"
   ["" {:name :home}]
   ["documents" {:name :documents}]
   ["pdf" {:name :pdf}]])

(def router
  (rfe/router
    routes
    {:data {:coercion rss/coercion}}))

(defn on-navigate
  [new-match]
  (when new-match
    (dispatch [:navigated new-match])))

(defn init-routes! 
  []
  (js/console.log "Initializing Routes")
  (rfee/start!
    router
    on-navigate
    {:use-fragment true}))