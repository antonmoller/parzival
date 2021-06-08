(ns parzival.views.pdf
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [parzival.views.highlight-toolbar :refer [highlight-toolbar]]
   [parzival.views.pagemark-menu :refer [pagemark-menu]]
   [parzival.style :refer [ZINDICES DEPTH-SHADOWS SCROLLBAR color]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

;;; Styles

(def pdf-container-style
  {:position "absolute"
   :height "100%"
   :overflow-x "hidden"
   :overflow-y "scroll"
   :padding-right "50px"
   :scrollbar-width "none"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode {"::-webkit-scrollbar" {:display "none"}}})


(def scroll-container-style 
  {:position "relative"
   :z-index (:zindex-sticky ZINDICES)
   :width "870px"
   :height "100%"
   ::stylefy/sub-styles {:scrollbar {:position "absolute"
                                     :top 0
                                     :bottom 0
                                     :right 0
                                     :width "32px"
                                     :border (:border SCROLLBAR)
                                     :box-shadow (:shadow SCROLLBAR)}
                         :thumb {:position "absolute"
                                 :z-index 2
                                 :left 0
                                 :top 0
                                 :height "0px"
                                 :width "inherit"
                                 :background (:thumb-color SCROLLBAR)
                                 :box-shadow (:thumb-shadow SCROLLBAR)
                                 :transition "background 0.8s linear"}}
   ::stylefy/manual [[:&:hover [:.thumb {:background (:thumb-visible-color SCROLLBAR)
                                         :transition "background 0.2s linear"}]]
                     [:.thumb:hover {:background (:thumb-hover-color SCROLLBAR)}]
                     [:.thumb:active {:background (:thumb-active-color SCROLLBAR)}]]})

;;; Components

(defn scrollbar
  [content]
  (let [scrollbar (subscribe [:pdf/scrollbar])
        top (r/atom 0)
        new-pos (fn [y]
                  (let [new-y  (cond
                                 (< y 0) 0
                                 (> y (:bottom-limit @scrollbar)) (:bottom-limit @scrollbar)
                                 :else y)]
                    (.scroll (:container @scrollbar) 0 (* (:scaling @scrollbar) new-y))
                    (reset! top new-y)))
        pointer-move-handler #(new-pos (+ @top (.-movementY %)))
        pointer-up-handler (fn [e]
                             (doto (.-target e)
                               (.removeEventListener "pointermove" pointer-move-handler)
                               (.releasePointerCapture (.-pointerId e))))
        pointer-down-handler (fn [e]
                               (case (.-buttons e)
                                 1 (if (.contains (.. e -target -classList) "thumb")
                                     (doto (.-target e)
                                       (.addEventListener "pointermove" pointer-move-handler)
                                       (.addEventListener "pointerup" pointer-up-handler (js-obj "once" true))
                                       (.setPointerCapture (.-pointerId e)))
                                     (new-pos (.-clientY e)))
                                 2 (js/console.log "right")))
        scroll-handler (fn [e]
                         (->> (/ (.. e -target -scrollTop) (:pdf-height @scrollbar))
                             (* (:track-height @scrollbar))
                             (reset! top)))]
     (fn []
       [:div#scrollWrapper (merge (use-style scroll-container-style)
                                  {:on-scroll scroll-handler
                                   :on-context-menu #(.preventDefault %)})
        content
        [:div.scrollbar (merge (use-sub-style scroll-container-style :scrollbar)
                               {:on-pointer-down pointer-down-handler})
         [:div.thumb (merge (use-sub-style scroll-container-style :thumb)
                            {:style {:top @top
                                     :height (str (:thumb-height @scrollbar) "px")}})]
         [:div {:style {:position "absolute"
                        :background "blue"
                        :z-index 1
                        :opacity 0.5
                        :width "30%"
                        :height "200px"
                        :top "500px"}}]
         [:div {:style {:position "absolute"
                        :background "red"
                        :z-index 1
                        :opacity 0.5
                        :width "30%"
                        :height "200px"
                        :top "700px"}}]]])))

(defn pdf
  []
  (let [pdf? (subscribe [:pdf?])
        url "https://arxiv.org/pdf/2006.06676v2.pdf"
        ; url "http://ltu.diva-portal.org/smash/get/diva2:1512634/FULLTEXT01.pdf"
        ]
    (fn []
      (dispatch [:pdf/load url])
      (when @pdf?
        (dispatch [:pdf/view]))
      [scrollbar
       [:div#viewerContainer (use-style pdf-container-style)
        [highlight-toolbar]
        [pagemark-menu]
        [:div#viewer.pdfViewer {:on-mouse-up #(dispatch [:highlight/toolbar-create])
                                :on-context-menu (fn [e]
                                                   (.preventDefault e)
                                                   (dispatch [:pagemark/menu (.-target e) (.-clientX e) (.-clientY e)]))}]]])))