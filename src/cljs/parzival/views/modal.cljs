(ns parzival.views.modal
  (:require
            [re-frame.core :refer [dispatch]]
            [stylefy.core :as stylefy :refer [use-style]]))

(def container-style
  {:position "fixed"
   :z-index "1020"
   :width "100%"
   :height "100%"})

(def backdrop-style
  {:position "fixed"
   :z-index "1"
   :width "100%"
   :height "100%"
   :background-color "black"
   :opacity "0.6"})

(def modal-style
  {:position "fixed"
   :z-index "2"
   :margin "auto"
   :width "200px"
   :height "125px"
   :background-color "white"
   :padding "16px"
   :border-radius "6px"})

(defn modal
  [toggle]
  [:div (use-style container-style)
   [:div (use-style backdrop-style {:on-click #(dispatch [toggle])})]
   [:div (use-style modal-style)
    [:h1 "testing this shit out"]]])
; (defn modal
;   [handler-fn]
;   [:div (use-style container-style)
;    [:div (use-style backdrop-style {:on-click handler-fn})]
;    [:div (use-style modal-style)
;     [:h1 "testing this out"]]])
