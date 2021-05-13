(ns parzival.events
  (:require
   [re-frame.core :as re-frame :refer [dispatch]]
   [parzival.db :as db]
  [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 :left-sidebar/toggle
 (fn [db _]
   (update db :left-sidebar/open not)))

(re-frame/reg-event-db
 :right-sidebar/toggle
 (fn [db _]
   (update db :right-sidebar/open not)))

(re-frame/reg-event-db
 :right-sidebar/set-width
 (fn [db [_ width]]
   (assoc db :right-sidebar/width width)))

(re-frame/reg-event-db
 :settings/toggle
 (fn [db _]
   (update db :settings/open not)))

(re-frame/reg-event-fx
 :search/toggle
 (fn [{:keys [db]} _]
   (let [anchor (get db :search/anchor)]
     {:db (if (nil? anchor)
            (assoc db :search/anchor {:left "50%" :top "50%"})
            (assoc db :search/anchor nil))})))

(re-frame/reg-event-db
 :theme/switch
 (fn [db _]
   (update db :theme/dark not)))

(re-frame/reg-event-db
 :loading/progress
 (fn [db prog]
   (assoc db :loading/progress prog)))

(re-frame/reg-event-fx
 :modal/handle-click
 (fn [_ [_ id toggle]]
   (js/console.log)
   (let [modal (.getElementById js/document id)]
     (.addEventListener js/document "mousedown" (fn handle-click [e]
                                                  (when-not (.contains modal (.-target e))
                                                    (.removeEventListener js/document "mousedown" handle-click)
                                                    (dispatch [toggle])))))))