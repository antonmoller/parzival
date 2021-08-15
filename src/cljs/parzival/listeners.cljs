(ns parzival.listeners
  (:require
   [re-frame.core :refer [dispatch reg-fx reg-event-fx]]))

(reg-fx
 :fs/save-before-quit!
 (fn [_]
   (.addEventListener js/window "beforeunload" (fn [e]
                                                 (dispatch [:quit/desktop])
                                                 (set! (.-returnValue e) false)))))

(reg-event-fx
 :listeners/init
 (fn [_ _]
   {:fx [[:fs/save-before-quit!]]}))