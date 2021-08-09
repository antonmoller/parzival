(ns parzival.events
  (:require
   [re-frame.core :as re-frame :refer [dispatch reg-event-db reg-event-fx]]
   [parzival.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(reg-event-db
 :left-sidebar/toggle
 (fn [db _]
   (update db :left-sidebar/open not)))

(reg-event-db
 :right-sidebar/toggle
 (fn [db _]
   (update db :right-sidebar/open not)))

(reg-event-db
 :right-sidebar/set-width
 (fn [db [_ width]]
   (assoc db :right-sidebar/width width)))

(reg-event-db
 :settings/toggle
 (fn [db _]
   (update db :settings/open not)))

(reg-event-db
 :search/toggle
 (fn [db _]
   (update db :search/open? not)))

(reg-event-db
 :filesystem/toggle
 (fn [db _]
   (update db :filesystem/open? not)))

(reg-event-db
 :theme/switch
 (fn [db _]
   (update db :theme/dark not)))

(reg-event-db
 :loading/progress
 (fn [db prog]
   (assoc db :loading/progress prog)))

(reg-event-fx
 :modal/handle-click
 (fn [_ [_ id toggle]]
   (let [modal (.getElementById js/document id)]
     (.addEventListener js/document "mousedown" (fn handle-click [e]
                                                  (when-not (.contains modal (.-target e))
                                                    (.removeEventListener js/document "mousedown" handle-click)
                                                    (dispatch [toggle])))))))