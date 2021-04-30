(ns parzival.views.highlight-toolbar
  (:require 
   [re-frame.core :refer [dispatch subscribe]]
   ["@material-ui/icons/CancelRounded" :default CancelRounded]
   ["@material-ui/icons/Brightness1Rounded" :default Brightness1Rounded]
   [stylefy.core :as stylefy :refer [use-style]]
   [parzival.style :refer [HIGHLIGHT-COLOR DEPTH-SHADOWS ZINDICES color]]))

(def TOOLBAR-WIDTH 120)

;;; Styles

(def toolbar-style
  {:visibility "hidden"
   :width (str TOOLBAR-WIDTH "px")
   :height "35px"
   :background-color (color :background-plus-1-color)
   :box-shadow    (str (:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower))
   :color (color :body-text-color)
   :border-radius "0.25rem"
   :position "absolute"
   :display "flex"
   :justify-content "space-evenly"
   :align-items "center"
   :z-index (:zindex-tooltip ZINDICES)
   :line-height 1})

(def anchor-style
  {:content "''"
   :position "absolute"
   :z-index (:zindex-tooltip ZINDICES)
   :color (color :background-plus-1-color)
   :margin-left "-5px"
   :border-width "5px"
   :border-style "solid"
   :border-color (str "transparent transparent " (color :background-plus-1-color) " transparent")})

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
  [{:keys [page-left page-right anchor-x _]}]
  (cond
    (< 0 (- (/ TOOLBAR-WIDTH 2) anchor-x)) "10px"
    (< page-right (+ page-left anchor-x (/ TOOLBAR-WIDTH 2))) (- page-right TOOLBAR-WIDTH 10)
    :else (str (- anchor-x (/ TOOLBAR-WIDTH 2)) "px")))


;;; Components

(defn highlight-toolbar
  []
  (let [position   (subscribe [:highlight/anchor])
        edit?      (subscribe [:highlight/edit])]
    (fn []
      [:div
       [:div (merge (use-style anchor-style)
                    {:style
                     {:visibility "visible"
                      :left (str (:anchor-x @position) "px")
                      :top  (str (- (:anchor-y @position) 10) "px")}})]
       [:div (merge (use-style toolbar-style)
                    {:style (if (some? @position)
                              {:visibility "visible"
                               :left (get-position @position)
                               :top  (str (:anchor-y @position) "px")}
                              {:visibility "hidden"})})
        (doall
         (for [[_ {:keys [color opacity]}] HIGHLIGHT-COLOR]
           (if (and (some? edit?) (= (:color @edit?) color))
             [:div (merge (use-style button-style)
                          {:key (str "highlight-" color)
                           :on-click #(dispatch [:highlight/remove])})
              [:> CancelRounded {:style {:color color}}]]
             [:div (merge (use-style button-style)
                          {:key (str "highlight-" color)
                           :on-click #(if (nil? @edit?)
                                        (dispatch [:highlight/add color opacity])
                                        (dispatch [:highlight/edit color opacity]))})
              [:> Brightness1Rounded  {:style {:color color}}]])))]])))
                          
