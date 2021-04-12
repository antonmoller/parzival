(ns parzival.style
  (:require
            [clojure.string]
            [re-frame.core :refer [subscribe]]
            [stylefy.core :as stylefy]))

(def THEME-DARK
  {:background-color          "rgba(30, 30, 30, 1)"          ; "#1E1E1E"
   :background-plus-1-color   "rgba(37, 37, 38, 1)"         ; "#252526"
   :left-sidebar-color        "rgba(51, 51, 51, 1)"         ; "#333333"
   :border-color              "rgba(55, 60, 63, 1)"        ; "#373C3F"
   :header-text-color         "rgba(186, 186, 186, 1)"         ; "#BABABA"
   :body-text-color           "rgba(170, 170, 170, 1)"        ; "#AAA"
   :link-color                "rgba(100, 141, 174, 1)"   ; "#648DAE"
   :highlight-color           "rgba(251, 190, 99, 1)"       ; "#FBBE63"
   :secondary-color           "rgba(170, 100, 123, 1)"        ; "#AA647B"
   :warning-color             "rgba(211, 47, 47, 1)"      ; "#D32F2F"
   :confirmation-color        "rgba(56, 142, 60, 1)"})      ; "#388E3C"})

(def THEME-LIGHT
  {:background-color          "rgba(30, 30, 30, 1)"       ; "#1E1E1E"
   :background-plus-1-color   "rgba(37, 30, 38, 1)"      ; "#252526"
   :left-sidebar-color        "rgba(51, 51, 51, 1)"      ; "#333333"
   :border-color              "rgba(55, 60, 63, 1)"     ; "#373C3F"
   :header-text-color         "rgba(186, 186, 186, 1)"         ; "#BABABA"
   :body-text-color           "rgba(170, 170, 170, 1)"        ; "#AAA"
   :link-color                "rgba(100, 141, 174, 1)"   ; "#648DAE"
   :highlight-color           "rgba(251, 190, 99, 1)"       ; "#FBBE63"
   :secondary-color           "rgba(170, 100, 123, 1)"        ; "#AA647B"
   :warning-color             "rgba(211, 47, 47, 1)"      ; "#D32F2F"
   :confirmation-color        "rgba(56, 142, 60, 1)"})        ; "#388E3C"})
  ; {:background-color        "#FFFFFF"
  ;  :background-plus-1-color "#F3F3F3"
  ;  :left-sidebar-color      "#2C2C2C"
  ;  :body-text-color         "#4333F38"})

(def HIGHLIGHT-COLOR
  {;;  :red    "rgba(178, 35, 49, 1)"; 0,5)"
   ;;  :yellow "rgba(255, 200, 101, 0.8)"
   :orange {:color "rgb(235, 97, 35)" :opacity 0.7}
   :green  {:color "rgb(101, 255, 200)" :opacity 1}
   :blue   {:color "rgb(101, 156, 255)" :opacity 0.8}
   :purple {:color "rgb(200, 101, 255)" :opacity 0.6}})

(def DEPTH-SHADOWS
  {:4                  "0px 1.6px 3.6px rgba(0, 0, 0, 0.13), 0px 0.3px 0.9px rgba(0, 0, 0, 0.1)"
   :8                  "0px 3.2px 7.2px rgba(0, 0, 0, 0.13), 0px 0.6px 1.8px rgba(0, 0, 0, 0.1)"
   :16                 "0px 6.4px 14.4px rgba(0, 0, 0, 0.13), 0px 1.2px 3.6px rgba(0, 0, 0, 0.1)"
   :64                 "0px 24px 60px rgba(0, 0, 0, 0.15), 0px 5px 12px rgba(0, 0, 0, 0.1)"})


(def OPACITIES
  {:opacity-lower  0.10
   :opacity-low    0.25
   :opacity-med    0.50
   :opacity-high   0.75
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
  "Turns a color and optional opacity into a CSS variable.
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
   :line-height "1.5"
   ::stylefy/manual  [[:a {:color (color :link-color)}]
                      [:h1 :h2 :h3 :h4 :h5 :h6 {:margin      "0.2em 0"
                                                :line-height "1.3"
                                                :color       (color :header-text-color)}]
                      [:h1 {:font-size      "3.125em"
                            :font-weight    600
                            :letter-spacing "-0.03em"}]
                      [:h2 {:font-size      "2.375em"
                            :font-weight    500
                            :letter-spacing "-0.03em"}]
                      [:h3 {:font-size      "1.75em"
                            :font-weight    500
                            :letter-spacing "-0.02em"}]
                      [:h4 {:font-size "1.3125em"}]
                      [:h5 {:font-size      "0.75em"
                            :font-weight    500
                            :line-height    "1em"
                            :letter-spacing "0.08em"
                            :text-transform "uppercase"}]
                      [:.MuiSvgIcon-root {:font-size "1.5rem"}]
                      [:input {:font-family "inherit"}]
                      [:kbd {:text-transform "uppercase"
                             :font-family    "inherit"
                             :font-size      "0.85em"
                             :letter-spacing "0.05em"
                             :font-weight    600
                             :display        "inline-flex"
                             :background     (color :body-text-color :opacity-lower)
                             :border-radius  "0.25rem"
                             :padding        "0.25em 0.5em"}]
                      [:img {:max-width "100%"
                             :height    "auto"}]
                      [":focus" {:outline-width 0}]
                      [":focus-visible" {:outline-width "1px"}]]})

(def app-styles
  {:overflow "hidden"
   :height "100vh"
   :width "100vw"})

(defn opacify
  [color opacity]
  (clojure.string/replace color #"(.)\)" (str opacity ")")))


(defn permute-color-opacities
  "Permutes all colors and opacities.
  There are 5 opacities and 12 colors. There are 72 keys (includes default opacity, 1.0)"
  [theme]
  (->> theme
       (mapcat (fn [[color-k color-v]]
                 (concat [(keyword (str "--" (symbol color-k)))
                          color-v]
                         (mapcat (fn [[opacity-k opacity-v]]
                                   [(keyword (str "--"
                                                  (symbol color-k)
                                                  "---"
                                                  (symbol opacity-k)))
                                    (opacify color-v opacity-v)])
                                 OPACITIES)
                         )))
       (apply hash-map)))

(defn init
  []
  (stylefy/init)
  (stylefy/tag "html" base-styles)
  ; (stylefy/tag "*" {:box-sizing "border-box"}) ;TODO
  (let [permute-light (permute-color-opacities THEME-LIGHT)
        permute-dark  (permute-color-opacities THEME-DARK)]
    (stylefy/tag ":root" (merge permute-light
                                {::stylefy/media {{:prefers-color-scheme "dark"} permute-dark}}))))
