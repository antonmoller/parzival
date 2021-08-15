(ns parzival.views
  (:require
   [parzival.views.modal.modal :refer [modal]]
   [parzival.views.right-sidebar :refer [right-sidebar]]
   [parzival.views.app-toolbar :refer [app-toolbar]]
   [parzival.views.pdf.pdf :refer [pdf]]
   [parzival.views.document-table :refer [document-table]]
   [parzival.views.left-sidebar :refer [left-sidebar]]
   [stylefy.core :as stylefy :refer [use-style]]
   [re-frame.core :refer [subscribe]]
   [parzival.subs]))

;; Styles

(def app-wrapper-style
  {:display "grid"
   :grid-template-areas
   "'left-header main-content right-header'
    'left-sidebar main-content secondary-content'"
   :grid-template-columns "auto 1fr auto"
   :grid-column-gap "2.5rem"
   :grid-template-rows "auto 1fr"
   :height "100vh"})

(def main-content-style
  {:grid-area "main-content"
   :display "flex"
   :justify-content "center"})

;; Components

(defn main-panel
  []
  (let [route-name (subscribe [:current-route/name])]
    (fn []
      [:div (use-style app-wrapper-style)
       [modal]
       [app-toolbar]
       [left-sidebar]
       [:div (use-style main-content-style)
        [document-table (= :documents @route-name)]
        [pdf (= :pdf @route-name)]]
       [right-sidebar]])))
