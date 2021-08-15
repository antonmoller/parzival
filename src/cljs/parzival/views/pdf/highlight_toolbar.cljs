(ns parzival.views.pdf.highlight-toolbar
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   ["@material-ui/icons/CancelRounded" :default CancelRounded]
   ["@material-ui/icons/Brightness1Rounded" :default Brightness1Rounded]
   [stylefy.core :as stylefy :refer [use-style]]
   [parzival.style :refer [HIGHLIGHT-COLOR DEPTH-SHADOWS ZINDICES color]]))

(def TOOLBAR-WIDTH 120)

;;; Styles

(def modal-style 
  {:z-index (:zindex-modal ZINDICES)
   :position "absolute"
   :box-shadow (str (:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower))})

(def toolbar-style
  (merge modal-style
         {:width (str TOOLBAR-WIDTH "px")
          :height "35px"
          :margin-top "5px"
          :margin-left "10px"
          :border-radius "0.25rem"
          :background-color (color :background-plus-1-color)
          :color (color :body-text-color)
          :display "flex"
          :justify-content "space-evenly"
          :align-items "center"
          :line-height 1}))

(def anchor-style
  (merge modal-style
         {:content "''"
          :margin-left "-5px"
          :border-width "5px"
          :border-style "solid"
          :color (color :background-plus-1-color)
          :border-color (str "transparent transparent " (color :background-plus-1-color) " transparent")}))

(def button-style 
  {:cursor "pointer"
   :height "100%"
   :width "100%"
   :display "flex"
   :justify-content "center"
   :align-items "center"
   ::stylefy/mode {:hover {:background (color :body-text-color :opacity-low)}}})

;;; Helpers

(defn get-position
  [{:keys [left page-right]}]
  (let [left-corner (- left (/ TOOLBAR-WIDTH 2))
        right-corner (+ left (/ TOOLBAR-WIDTH 2))]
    (cond
      (> 0 left-corner) "10px"
      (< right-corner page-right) (str (- right-corner TOOLBAR-WIDTH 10) "px")
      :else (str left-corner "px"))))

;;; Components

(defn highlight-toolbar
  []
  (let [pos   @(subscribe [:highlight/anchor])
        edit? @(subscribe [:highlight/edit])]
    (when (some? pos)
      (dispatch [:highlighting/close])
      [:div#highlight-toolbar
       [:div (merge (use-style anchor-style)
                    {:style {:left (str (:left pos) "px")
                             :top (str (- (:top pos) 5) "px")}})]
       [:div (merge (use-style toolbar-style)
                    {:style {:top (str (:top pos) "px")
                             :left (get-position pos)}})
        (doall
         (for [[k {color :color}] HIGHLIGHT-COLOR]
           (if (and (some? edit?) (= (:color edit?) color))
             [:div (merge (use-style button-style)
                          {:key (str "highlight-" color)
                           :on-mouse-down #(dispatch [:highlight/remove])})
              [:> CancelRounded {:style {:color color}}]]
             [:div (merge (use-style button-style)
                          {:key (str "highlight-" color)
                           :on-mouse-down #(if (nil? edit?)
                                             (dispatch [:highlight/add k])
                                             (dispatch [:highlight/edit k]))})
              [:> Brightness1Rounded  {:style {:color color}}]])))]])))