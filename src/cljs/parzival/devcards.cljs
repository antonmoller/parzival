(ns parzival.devcards
  (:require
            [devcards.core]
            [parzival.devcards.right-sidebar]
            [parzival.events :as events]
            [parzival.subs]
            [parzival.style :as style]
            [re-frame.core :refer [dispatch-sync]]
            [stylefy.core :as stylefy]))

(defn ^:export main
  []
  (style/init)
  (dispatch-sync [::events/initialize-db])
  (devcards.core/start-devcard-ui!))
             
