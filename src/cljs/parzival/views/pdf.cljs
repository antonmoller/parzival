(ns parzival.views.pdf
  (:require
            [re-frame.core :refer [dispatch subscribe]]
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
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode {"::-webkit-scrollbar" {:display "none"} ; TODO IE, Edge, Firefox
                   }})


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

;;; Components

(defn scrollbar
  [content]
  [:div (use-style scroll-container-style)
   content
   [:div.scrollbar (use-sub-style scroll-container-style :scrollbar)
    [:div.thumb (use-sub-style scroll-container-style :thumb)]]])

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