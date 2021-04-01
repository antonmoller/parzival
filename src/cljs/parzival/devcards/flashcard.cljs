(ns parzival.devcards.flashcard
  (:require
   [parzival.views.flashcard :refer [flashcard]]
   [devcards.core :refer [defcard-rg]]))

(defcard-rg Flashcard
[:div {:style {:display "flex" :justify-content "center"}}
  [flashcard]])
