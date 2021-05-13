(ns parzival.views
  (:require
   [parzival.views.right-sidebar :refer [right-sidebar]]
   [parzival.views.settings :refer [settings]]
   [parzival.views.search :refer [search]]
   [parzival.views.app-toolbar :refer [app-toolbar]]
   [parzival.views.pdf :refer [pdf]]
   [parzival.views.document-table :refer [document-table]]
   [parzival.views.left-sidebar :refer [left-sidebar]]
   [stylefy.core :as stylefy :refer [use-style]]
   [re-frame.core :as rf :refer [subscribe]]
   [parzival.subs :as subs]))

;; Styles

(def app-wrapper-style
  {:display "grid"
   :grid-template-areas
   "'left-header  main-content right-header'
    'left-sidebar main-content secondary-content'"
   :grid-template-columns "auto 1fr auto"
   :grid-template-rows "auto 1fr"
   :height "100vh"})

(def main-content-style
  {:grid-area "main-content"
   :display "flex"
   :justify-content "center"})

;; Components

(defn match-panel
  [route-name]
   (case route-name
      :documents document-table
      :pdf       pdf
      [:div]))


(defn main-panel 
  []
  (let [route-name (subscribe [:current-route/name])]
    (fn []
      [:div (use-style app-wrapper-style)
        [search]
        ;; [settings]
        [app-toolbar]
        ;; [left-sidebar]
        [:div (use-style main-content-style)
         [pdf]
        ;; [match-panel @route-name]
         ]
        ;; [right-sidebar]
       ])))
