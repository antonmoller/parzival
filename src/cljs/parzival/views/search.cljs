(ns parzival.views.search
  (:require
            [parzival.views.modal :refer [modal]]
            [parzival.style :refer [color OPACITIES]]
            [re-frame.core :refer [subscribe]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def search-style
  {:display "flex"
   :flex-direction "column"
   :width "600px"
   :height "130px"})

(def input-style
  {:background-color "inherit"
   :color "inherit"
   :caret-color (color :link-color)
   :border "none"
   :font-size "2.375rem"
   :font-weight "300"
   :line-height "1.3"
   :letter-spacing "-0.03em"
   :border-radius "0.25rem 0.25rem 0 0"
   :padding "1.5rem 4rem 1.5rem 1.5rem"
   ::stylefy/mode {:focus {:outline "none"}
                   "::placeholder" {:color (color :body-text-color :opacity-low)}}}) 

(def result-style
  {:border-top (str "1px solid " (color :border-color))
   :padding "0.25rem 1.125rem"
   :display "flex"
   :flex-direction "column"
   :align-items "flex-end"})

(def hint-style
  {:color "inherit"
   :opacity (:opacity-med OPACITIES)
   :font-size "14px"})

;;; Components

(defn search
  []
  (let [open? (subscribe [:search/open])]
    (fn []
      (when @open?
        [modal :search/toggle
         [:div (use-style search-style)
         [:input (use-style input-style 
                            {:placeholder "Search"
                             :auto-focus true})]
          [:div (use-style result-style)
            [:div (use-style hint-style)
             [:span "Press "]
             [:kbd "shift + enter"]
             [:span " to open in new tab"]]]]]))))
