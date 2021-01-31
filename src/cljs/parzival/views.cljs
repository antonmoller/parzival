(ns parzival.views
  (:require
   [parzival.views.right-sidebar :refer [right-sidebar]]
   [parzival.views.settings :refer [settings]]
   [parzival.views.search :refer [search]]
   [parzival.views.app-toolbar :refer [app-toolbar]]
   [parzival.views.main-content :refer [main-content]]
   [parzival.views.left-sidebar :refer [left-sidebar]]
   [stylefy.core :as stylefy :refer [use-style]]
   [re-frame.core :as re-frame]
   [parzival.subs :as subs]))

;; Styles

(def app-wrapper-style
  {:display "grid"
   :grid-template-areas
   "'app-header app-header app-header'
    'left-sidebar main-content secondary-content'"
   :grid-template-columns "1fr auto 1fr"
   :grid-template-rows "auto 1fr"
   :height "100vh"})

;; Components

(defn main-panel 
  []
  [:div (use-style app-wrapper-style)
   ; [search]
   ; [settings]
   [app-toolbar]
   [left-sidebar]
   ; [main-content]
   ; [right-sidebar]
   ])
