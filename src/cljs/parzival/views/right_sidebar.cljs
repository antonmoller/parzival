(ns parzival.views.right-sidebar
  (:require
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [parzival.style :refer [color]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def right-sidebar-dragger-style
  {:width "3px"
   :cursor "col-resize"
   :background-color (color :border-color)})

(def right-sidebar-style
  {:grid-area "secondary-content"
   :justify-self "stretch"
   :display "flex"
   :align-items "stretch"
   :width "0"
   :height "100%"
   :resize "horizontal"
   :background-color (color :background-plus-1-color)
   :transition-property "width, border, background"
   :transition-duration "0.35s"
   :transition-timing-function "ease-out"
   ::stylefy/manual [[:&.is-open {:width "32vw"}]
                     [:&.is-closed {:width "0"}]]})

(def right-sidebar-content-style
  {})

;;; Components

(defn right-sidebar
  []
  (let [open? (subscribe [:right-sidebar/open])
        width (r/atom @(subscribe [:right-sidebar/width]))
        dragging? (r/atom false)
        handle-mousemove (fn [e]
                           (when @dragging?
                             (.. e preventDefault)
                             (let [x (.-clientX e)
                                   inner-w js/window.innerWidth
                                   new-width (-> (- inner-w x)
                                             (/ inner-w)
                                             (* 100))]
                               (reset! width new-width))))
        handle-mouseup (fn []
                         (when @dragging?
                           (reset! dragging? false)
                           (dispatch [:right-sidebar/set-width @width])))]
    (r/create-class
      {:display-name "right-sidebar"
       :component-did-mount (fn []
                              (js/document.addEventListener "mousemove" handle-mousemove)
                              (js/document.addEventListener "mouseup" handle-mouseup))
       :component-will-unmount (fn []
                                (js/document.removeEventListener "mousemove" handle-mousemove)
                                (js/document.removeEventListener "mouseup" handle-mouseup))
       :reagent-render (fn []
                         [:div (merge (use-style right-sidebar-style 
                                                 {:class (if @open? "is-open" "is-closed")})
                                      {:style (cond-> {}
                                                @dragging? (assoc :transition-duration "0s")
                                                @open? (assoc :width (str @width "vw")))})
                          [:div (use-style right-sidebar-dragger-style {:on-mouse-down #(reset! dragging? true)})]])})))
