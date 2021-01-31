(ns parzival.views.main-content
  (:require
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def main-content-style
  {:grid-area "main-content"
   :display "flex"
   :flex "1 1 100%"
   :justify-content "stretch"
   :align-items "flex-start"
   :overflow-y "auto"
   :padding-top "2.5rem"})

;;; Components

(defn main-content
  []
  [:div (use-style main-content-style)]) 
