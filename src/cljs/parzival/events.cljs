(ns parzival.events
  (:require
   [parzival.utils :refer [gen-uid check-spec]] 
   [re-frame.core :as re-frame :refer [dispatch reg-event-db reg-event-fx reg-sub reg-fx]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

;;; debounce
(defonce timeouts
  (atom {}))

(reg-fx
 :dispatch-debounce
 (fn [[id event-vec s]]
   (js/clearTimeout (@timeouts id))
   (swap! timeouts assoc id
          (js/setTimeout (fn []
                           (dispatch event-vec)
                           (swap! timeouts dissoc id))
                         (* 1000 s)))))

(reg-fx
 :stop-all-debounce
 (fn [_]
   (run! (fn [[_ v]] (js/clearTimeout v)) @timeouts)
   (reset! timeouts nil)))

;;; left-sidebar
(reg-sub
 :left-sidebar/open?
 (fn [db _]
   (:left-sidebar/open? db)))

(reg-event-db
 :left-sidebar/toggle
 (fn [db _]
   (update db :left-sidebar/open? not)))

;;; right-sidebar
(reg-sub
 :right-sidebar/open?
 (fn [db _]
   (:right-sidebar/open? db)))

(reg-event-db
 :right-sidebar/toggle
 (fn [db _]
   (update db :right-sidebar/open? not)))

(reg-sub
 :right-sidebar/width
 (fn [db _]
   (:right-sidebar/width db)))

(reg-event-db
 :right-sidebar/set-width
 (fn [db [_ width]]
   (assoc db :right-sidebar/width width)))

;;; theme
(reg-sub
 :theme/dark?
 (fn [db _]
   (:theme/dark db)))

(reg-event-db
 :theme/switch
 (fn [db _]
   (update db :theme/dark? not)))

;;; modal
(reg-sub
 :modal/content
 (fn [db _]
   (:modal/content db)))

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

;;; pages
(reg-sub
 :pages
 (fn [db _]
   (:pages db)))

(reg-event-fx
 :page/create
 (fn [{:keys [db]} [_ {:keys [title num-pages authors filename]}]]
   (let [uid (gen-uid "page")
         timestamp (.getTime (js/Date.))
         data (cond-> {:title title :modified timestamp :added timestamp :refs []}
                (some? filename) (assoc :filename filename :num-pages num-pages :authors authors
                                        :highlights {} :pagemarks {}))]
     {:db (->> (check-spec :parzival.db/page data)
               (assoc-in db [:pages uid]))})))

(reg-sub
 :page/active
 (fn [db _]
   (:page/active db)))

(reg-event-db
 :page/set-active
 (fn [db [_ uid]]
   (assoc db :page/active uid)))