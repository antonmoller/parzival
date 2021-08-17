(ns parzival.events
  (:require
   [re-frame.core :as re-frame :refer [dispatch reg-event-db reg-event-fx]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

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
 :theme/switch
 (fn [db _]
   (update db :theme/dark not)))

(reg-event-db
 :loading/progress
 (fn [db prog]
   (assoc db :loading/progress prog)))

(reg-event-db
 :modal/set-content
 (fn [db [_ content]]
   (assoc db :modal/content content)))

(reg-event-fx
 :modal/handle-click
 (fn []
   (let [modal (.getElementById js/document "modal")]
     (.addEventListener js/document "mousedown" (fn handle-click [e]
                                                  (when-not (.contains modal (.-target e))
                                                    (.removeEventListener js/document "mousedown" handle-click)
                                                    (dispatch [:modal/set-content nil])))))))