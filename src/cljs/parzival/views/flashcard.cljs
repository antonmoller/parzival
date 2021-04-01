(ns parzival.views.flashcard
  (:require
   [parzival.style :refer [color]]
   [parzival.views.buttons :refer [button]]
   [re-frame.core :refer [subscribe dispatch]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

 (def view-style
   {:display "flex"
    :flex-direction "column"
    :justify-content "space-between"
    :width "50rem"
    :height "40rem"
    :padding "5rem 2.5rem 2.5rem"
    :border (str "1px solid " (color :border-color))})
 
 (def button-row-style
   {:display "grid"
    :grid-auto-flow "column"
    :column-gap "0.25rem"
    :border-top (str "1px solid " (color :border-color))})


;;; Components

(defn flashcard
 []
 [:div (use-style view-style)
  [:span "testing this out"]
  [:div (use-style button-row-style)
   [button {:primary true} "Edit"]
   [button {:primary true} "Again"]
   [button {:primary true} "Hard"]
   [button {:primary true} "Good"]
   [button {:primary true} "Easy"]]])
