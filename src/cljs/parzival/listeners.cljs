(ns parzival.listeners
  (:require
   [re-frame.core :refer [dispatch-sync subscribe reg-fx reg-event-fx]]))


(reg-fx
 :fs/save-before-quit!
 (fn [_]
   (.addEventListener js/window "beforeunload" #(let [synced? @(subscribe [:db/synced?])]
                                                  (when-not synced?
                                                    (dispatch-sync [:stop-all-debounce])
                                                    (dispatch-sync [:db/synced])
                                                    (dispatch-sync [:db/sync]))))))

(reg-event-fx
 :listeners/init
 (fn []
   {:fs/save-before-quit! []}))