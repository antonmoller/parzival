(ns parzival.views.modal
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [parzival.style :refer [color ZINDICES DEPTH-SHADOWS]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def modal-style
  {:position "absolute"
   :z-index (:zindex-modal ZINDICES)
   :transform "translate(-50%, -50%)"
   :border-radius "0.25rem"
   :background-color (color :background-plus-1-color)
   :box-shadow [[(:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower)]]})

;;; Components

(defn modal
  [anchor toggle id content]
  (let [pos (subscribe [anchor])]
    (fn []
      (when (some? @pos)
        (dispatch [:modal/handle-click id toggle])
        [:div (merge (use-style modal-style)
                     {:id id
                      :style {:left (:left @pos)
                              :top  (:top @pos)}})
         content]))))

