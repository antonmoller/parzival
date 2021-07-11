(ns parzival.views.pdf
  (:require
   ["@material-ui/icons/Close" :default Close]
   ["@material-ui/icons/ArrowRightAlt" :default ArrowRightAlt]
   ["@material-ui/icons/Add" :default Add]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [parzival.views.highlight-toolbar :refer [highlight-toolbar]]
   [parzival.views.pagemark-menu :refer [pagemark-menu]]
   [parzival.views.buttons :refer [button]]
   [parzival.views.virtual-scrollbar :refer [virtual-scrollbar]]
   [parzival.style :refer [ZINDICES DEPTH-SHADOWS SCROLLBAR PAGEMARK-COLOR OPACITIES color]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

;;; Styles

(def pdf-container-style
  {:position "absolute"
   :height "100%"
   :padding-right "50px"
   :overflow-x "hidden"
   :overflow-y "scroll"
   :scrollbar-width "none"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode {"::-webkit-scrollbar" {:display "none"}}})

(def pagemark-style
  {:position "absolute"
   :z-index 2
   :top 0
   :right 0
   :width "360px"
   :height "100%"
   :visibility "hidden"
   :display "flex"
   :justify-content "space-between"
   :align-items "center"
   :pointer-events "none"
   })

(def pagemark-card-style
  {:width "250px"
  ;;  :margin "0 2rem"
   :margin-left "2.5rem"
   :pointer-events "auto"
   :border-radius "0.25rem"
   :background (color :background-plus-1-color :opacity-high)
   :backdrop-filter "blur(8px)"
   :box-shadow [[(:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower)]]
   ::stylefy/sub-styles {:content {:height "125px"
                                   :padding-bottom "1rem"
                                   :display "flex"
                                   :flex-direction "column"
                                   :justify-content "space-between"
                                   :align-items "center"}
                         :input {:width "50px"
                                 :background-color (color :left-sidebar-color)
                                 :opacity "0.7"
                                 :color "inherit"
                                 :caret-color (color :link-color)
                                 :font-size "1rem"
                                 :font-weight "300"
                                 :border-radius "0.25rem"
                                 :border "none"
                                 :line-height "1.3"
                                 :letter-spacing "-0.03em"
                                 :padding "0.5rem"
                                 ::stylefy/mode {:focus {:outline "none"}
                                                 "::placeholder" {:color (color :body-text-color :opacity-med)}}}
                         :line {:height "125px"
                                :width "20px"
                                :border "2px solid red"}}})

;;; Components

(defn pagemark-card
  [value]
  [:div (use-style pagemark-card-style)
   [:div (use-sub-style pagemark-card-style :content)
    [button {:on-click #(dispatch [:pagemark/sidebar-remove (:key value)])
             :style {:align-self "flex-start"}}
     [:> Close]]
    [:div {:style {:display "flex"
                   :justify-content "center"
                   :align-items "center"}}
     [:input (use-sub-style pagemark-card-style :input
                            {:placeholder "start"})]
     [:> ArrowRightAlt {:style {:font-size "2.375rem"}}]
     [:input (use-sub-style pagemark-card-style :input
                            {:placeholder "end"})]]
    [:input (merge (use-sub-style pagemark-card-style :input
                                  {:placeholder "deadline"})
                   {:style {:width "160px"}})]]])

(defn pagemark
  []
  (let [pagemark? (subscribe [:pagemark?])]
    (fn []
      [:div#createPagemark (merge (use-style pagemark-style)
                                  {:style {:visibility (if @pagemark? "visible" "hidden")}})
        ;; (into [:div] (map #(do ^{:key (:key %)} [pagemark-card %]) @pagemarks))]
       [pagemark-card]
       [:div {:style {:position "relative"
                      :z-index 1
                      :top 1
                      :right 1
                      :bottom 1
                      :height "100%"
                      :width "68px"
                      :border-left (str "1px solid " (color :background-plus-1-color))
                      :pointer-events "auto"}}
        [:div {:style {:position "absolute"
                       :top 0
                      ;;  :left scrollbar-width
                      ;;  :right 1
                       :height "100px"
                       :width "17px"
                       :right 0
                      ;;  :left 1
                       :background "rgba(255,0,0,0.5)"
                       :border-top "1px solid red"
                       :border-bottom "1px solid red"
                       :cursor "ns-resize"}}]
        [:div {:style {:position "absolute"
                       :top 200
                       :bottom 500
                       :width "100%"
                      ;;  :left 1
                       :right 0
                       :height "1px"
                       :background "rgba(0,255,0,0.5)"
                       :border-top "1px solid green"
                       :border-bottom "1px solid green"
                       :box-sizing "border-box"
                       :cursor "ns-resize"}}]
        [:div {:style {:position "absolute"
                       :top 600
                      ;;  :left 1
                      ;;  :right 0
                       :height "200px"
                       :width "100%"
                       :background "rgba(0,0,255,0.1)"
                       :border-top "1px solid blue"
                       :border-bottom "1px solid blue"
                       :cursor "ns-resize"}}]
        [:div {:style {:position "absolute"
                       :top 900
                      ;;  :left 1
                       :right 0
                      ;;  :width "100%"
                       :height "400px"
                       :width "17px"
                       :background "rgba(0,255,0,0.3)"
                       :cursor "not-allowed"}}]]])))

(def pagemark-sidebar-style
  {:position "absolute"
   :width "30%"
   :opacity "0.4"})

        ;; (into [:div] (map #(do ^{:key (:key %)} [pagemark-card %]) @pagemarks))]

(defn pagemark-sidebar-key
  [{:keys [_ top height]}]
  (str "pagemark-sidebar-" top "-" height))

(defn pagemark-sidebar
  [render?]
  (when render?
    (let [pagemarks (subscribe [:pagemarks])]
      (fn []
        (into [:div] (map #(do ^{:key (pagemark-sidebar-key %)}
                            [:div (merge (use-style pagemark-sidebar-style)
                                         {:style {:top (:top %)
                                                  :height (:height %)
                                                  :background "green"}})]) @pagemarks))))))


(defn pdf
  []
  (let [pdf? (subscribe [:pdf?])
        loading? (subscribe [:pdf/loading?])
        ;; pdf (subscribe [:pdf])
        url "https://arxiv.org/pdf/2006.06676v2.pdf"
        ; url "http://ltu.diva-portal.org/smash/get/diva2:1512634/FULLTEXT01.pdf"
        ]
    (fn []
      (when (and (not @pdf?) (not @loading?))
        (dispatch [:pdf/loading-set true])
        (dispatch [:pdf/load url]))
      (when @pdf?
        (dispatch [:pdf/view]))
      [virtual-scrollbar
       {:content  [:div#viewerContainer (use-style pdf-container-style)
                   [highlight-toolbar]
                   [pagemark-menu]
                   [:div#viewer.pdfViewer {:on-mouse-up #(dispatch [:highlight/toolbar-create])
                                           :on-context-menu (fn [e]
                                                              (.preventDefault e)
                                                              (dispatch [:pagemark/menu
                                                                         (.-target e)
                                                                         (.-clientX e)
                                                                         (.-clientY e)]))}]]
        :scroll-container-id "viewerContainer"
        :container-id "viewer"
        :container-width "855px"
        :content-overlay [pagemark]
        :scrollbar-content [pagemark-sidebar @pdf?]
        :scrollbar-width "20px"}])))