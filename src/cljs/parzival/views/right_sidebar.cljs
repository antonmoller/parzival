(ns parzival.views.right-sidebar
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [parzival.style :refer [color ZINDICES]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def sidebar-style
  {:grid-area "secondary-content"
   :justify-self "stretch"
   :width "0"
   :oveflow "hidden"
   :display "flex"
   :justify-content "space-between"
   :background-color (color :background-plus-1-color)
   :transition-property "width, border, background"
   :transition-duration "0.35s"
   :transition-timing-function "ease-out"
   ::stylefy/manual [[:&.is-open {:width "32vw"}]
                     [:&.is-closed {:width "0"}]]})

(def right-sidebar-dragger-style
  {:cursor "col-resize"
   :position "absolute"
   :z-index (:zindex-fixed ZINDICES)
   :top 0
   :width "7px"
   :height "100%"
   :background-color (color :border-color)})


(def sidebar-content-style
  {:display "flex"
   :flex "1 1 32vw"
   :position "absolute"
   :flex-direction "column"
   :margin-left "0"
   :oveflow-y "auto"
   :transition "all 0.35s ease-out"
   ::stylefy/manual [[:&.is-closed {:margin-left "-32vw"
                                    :opacity 0}]
                     [:&.is-open   {:opacity 1}]]})

; TODO: Margin left wtf????
(def page-style
  { ; :overflow "visible"
   :flex "1 1 100%"
   ; :flex-basis "100%"
   ; :padding-top "2.5rem"
   :margin "2.5rem"
   :padding "0.125rem 0.5rem"
   :cursor "text"
   :overflow "visible"
   :word-break "break-word"
   :display "flex"
   :justify-content "flex-start"
   :flex-direction "column"
   :line-height "2em"
   ; :border "2px solid rgba(255,255,255,0.6)"
   ; :content-editable "true"
   ; :content-editable "true"
   ; :display "flex"
   ; :flex-direction "column"

   })


;;; Components

(defn right-sidebar
  []
  (let [open? @(subscribe [:right-sidebar/open])
        width @(subscribe [:right-sidebar/width])]
    [:div#right-sidebar (merge (use-style sidebar-style
                                          {:class (if open? "is-open" "is-closed")})
                               {:style (if open? {:width width} {})})
     [:div (use-style right-sidebar-dragger-style {:on-pointer-down (fn [e]
                                                                      (.setPointerCapture (.. e -target -parentElement) (.-pointerId e))
                                                                      (dispatch [:resize
                                                                                 "right-sidebar"
                                                                                 "w-resize"
                                                                                 [:right-sidebar/set-width]]))})]
     [:div (use-style sidebar-content-style {:class (if open? "is-open" "is-closed")})
      [:div (use-style page-style {:content-editable "true"})]]]))
