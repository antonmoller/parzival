(ns parzival.devcards
  (:require
            [parzival.devcards.table]
            [parzival.devcards.flashcard]
            [parzival.devcards.app-toolbar]
            [parzival.devcards.left-sidebar]
            [parzival.devcards.right-sidebar]
            [parzival.devcards.modal]
            [devcards.core]
            [parzival.events :as events]
            [parzival.subs]
            [parzival.style :as style]
            [re-frame.core :refer [dispatch-sync]]))

(defn ^:export main
  []
  (style/init)
  (dispatch-sync [::events/initialize-db])
  (devcards.core/start-devcard-ui!))
             
