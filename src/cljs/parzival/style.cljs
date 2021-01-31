(ns parzival.style
  (:require
            [stylefy.core :as stylefy]
            [garden.color :refer [opacify hex->hsl]]
            [re-frame.core :refer [subscribe]]))

(def THEME-DARK
  {:background-color        "#1E1E1E"
   :background-plus-1-color "#252526"
   :left-sidebar-color      "#333333"
   :border-color            "#373C3F"
   :header-text-color       "#BABABA"
   :body-text-color         "#AAA"
   :link-color              "#2399E7"
   :highlight-color         "#FBBE63"})

(def THEME-LIGHT
  {:background-color        "#FFFFFF"
   :background-plus-1-color "#F3F3F3"
   :left-sidebar-color      "#2C2C2C"
   :body-text-color         "#4333F38"})

   ; :warning-color
   ; :confirmation-color
   ; :border-color
   ; :background-color})

(def OPACITIES
  {:opacity-lower 0.10
   :opacity-low 0.25
   :opacity-med 0.50
   :opacity-high 0.75
   :opacity-higher 0.85})

(def ZINDICES
  {:zindex-dropdown         1000
   :zindex-sticky           1020
   :zindex-fixed            1030
   :zindex-modal-backdrop   1040
   :zindex-modal            1050
   :zindex-popover          1060
   :zindex-tooltip          1070})

(defn color
  "Turns a color and optional opacity into a css variable.
  Only accepts keywords."
  ([variable]
   (when (keyword? variable)
     (str "var(--" (symbol variable) ")")))
  ([variable alpha]
   (when (and (keyword? variable) (keyword? alpha))
     (str "var(--" (symbol variable) "---" (symbol alpha) ")"))))

(def base-styles
  {:background-color (color :background-color)
   :font-family "Roboto, sans-serif"
   :color (color :body-text-color)
   :font-size "16px"
   :line-height "1.5"})

(def app-styles
  {:overflow "hidden"
   :height "100vh"
   :width "100vw"})

(defn permute-color-opacities
  "Permutes all color and opacities."
  [theme]
  (->> theme
       (mapcat (fn [[color-k color-v]]
                 (concat [(keyword (str "--" (symbol color-k))) color-v]
                         (mapcat (fn [[opacity-k opacity-v]]
                                   [(keyword (str "--" 
                                                  (symbol color-k) 
                                                  "---" 
                                                  (symbol opacity-k)))
                                    (opacify (hex->hsl color-v) opacity-v)])
                                 OPACITIES))))
       (apply hash-map)))

(defn init
  []
  (stylefy/init)
  (stylefy/tag "html" base-styles)
  (stylefy/tag "*" {:box-sixing "border-box"})
  (let [permute-light (permute-color-opacities THEME-LIGHT)
        permute-dark  (permute-color-opacities THEME-DARK)]
    (stylefy/tag ":root" (merge permute-light
                                {::stylefy/media {{:prefers-color-scheme "dark"} permute-dark}}))))
