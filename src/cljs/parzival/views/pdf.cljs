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
                                 :height "75px"
                                 :width "inherit"
                                 :background (:thumb-color SCROLLBAR)
                                 :box-shadow (:thumb-shadow SCROLLBAR)
                                 :transition "background 0.8s linear"}}
   ::stylefy/manual [[:&:hover [:.thumb {:background (:thumb-visible-color SCROLLBAR)
                                         :transition "background 0.2s linear"}]]
                     [:.thumb:hover {:background (:thumb-hover-color SCROLLBAR)}]
                     [:.thumb:active {:background (:thumb-active-color SCROLLBAR)}]]})

;;; Helpers

(defn compute-height
  [id]
  (as-> (.getElementById js/document id) res
    (.getComputedStyle js/window res)
    (.getPropertyValue res "height")
    (js/parseInt res)))

;;; Components

(defn scrollbar
  [content]
  (let [state (r/atom {:thumb-top 0
                       :thumb-height 44})
        pointer-move-handler (fn [e]
                               (let [new-y-tmp (+ (:thumb-top @state) (.-movementY e))
                                     scrollbar-height (.. js/document -documentElement -clientHeight)
                                     t-height (:thumb-height @state)
                                     viewer-container (.getElementById js/document "viewerContainer")
                                     scaling (/ (compute-height "viewer") (compute-height "viewerContainer"))
                                     new-y  (cond
                                              (< new-y-tmp 0) 0
                                              (> (+ new-y-tmp t-height) scrollbar-height) (- scrollbar-height t-height)
                                              :else new-y-tmp)]
                                 (.scroll viewer-container 0 (* scaling new-y))
                                 (swap! state assoc :thumb-top new-y)))
        pointer-down-handler (fn [e]
                               (when (= (.-buttons e) 1)
                                 (doto (.-target e)
                                   (.addEventListener "pointermove" pointer-move-handler)
                                   (.setPointerCapture (.-pointerId e)))))
        pointer-up-handler (fn [e]
                             (doto (.-target e)
                               (.removeEventListener "pointermove" pointer-move-handler)
                               (.releasePointerCapture (.-pointerId e))))
        pointer-down-track-handler (fn [e]
                                     (when-not (.contains (.. e -target -classList) "thumb")
                                       (let [new-y-tmp  (.-clientY e)
                                             scrollbar-height (.. js/document -documentElement -clientHeight)
                                             t-height (:thumb-height @state)
                                             container (.getElementById js/document "viewerContainer")
                                             scaling (/ (compute-height "viewer") (compute-height "viewerContainer"))
                                             new-y  (cond
                                                      (< new-y-tmp 0) 0
                                                      (> (+ new-y-tmp t-height) scrollbar-height) (- scrollbar-height t-height)
                                                      :else new-y-tmp)]
                                         (.scroll container 0 (* scaling new-y))
                                         (swap! state assoc :thumb-top new-y))))
        scroll-handler (fn [e]
                         (let [scrollbar-height (.. js/document -documentElement -clientHeight)
                               height-percent (/ (.. e -target -scrollTop) (compute-height "viewer"))]
                           (swap! state assoc :thumb-top (* height-percent scrollbar-height))))]
    (fn []
      [:div (merge (use-style scroll-container-style)
                   {:on-scroll scroll-handler})
       content
       [:div.scrollbar (merge (use-sub-style scroll-container-style :scrollbar)
                              {:on-pointer-down pointer-down-track-handler})
        [:div.thumb (merge (use-sub-style scroll-container-style :thumb)
                           {:style {:top (:thumb-top @state)
                                    :height (str (:thumb-height @state) "px")}
                            :on-pointer-down pointer-down-handler
                            :on-pointer-up pointer-up-handler})]]])))

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