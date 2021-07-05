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
   [parzival.style :refer [ZINDICES DEPTH-SHADOWS SCROLLBAR PAGEMARK-COLOR OPACITIES color]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

;;; Styles

(def hide-scrollbar
  {:overflow-y "auto"
   :scrollbar-width "none"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode {"::-webkit-scrollbar" {:display "none"}}})

;; (def scrollbar-width "32px")
(def scrollbar-width "20px")

(def pdf-container-style
  (merge hide-scrollbar
         {:position "absolute"
          :height "100%"
          :overflow-x "hidden"
          :overflow-y "scroll"
          :padding-right "50px"}))


(def scroll-container-style 
  {:position "relative"
   :z-index (:zindex-sticky ZINDICES)
   :width "855px"
   :height "100%"
   ::stylefy/sub-styles {:scrollbar {:position "absolute"
                                     :right 0
                                     :height "100%"
                                     :box-sizing "border-box"
                                     :width scrollbar-width
                                     :border (:border SCROLLBAR)
                                     :box-shadow (:shadow SCROLLBAR)}
                         :thumb {:position "absolute"
                                 :z-index 1
                                 :left 0
                                 :top 0
                                 :height "0px"
                                 :width "100%"
                                 :background (:thumb-color SCROLLBAR)
                                 :box-shadow (:thumb-shadow SCROLLBAR)
                                 :transition "background 0.8s linear"}}
   ::stylefy/manual [[:&:hover [:.thumb {:background (:thumb-visible-color SCROLLBAR)
                                         :transition "background 0.2s linear"}]]
                     [:.thumb:hover {:background (:thumb-hover-color SCROLLBAR)}]
                     [:.thumb:active {:background (:thumb-active-color SCROLLBAR)}]]})

(def pagemark-style
  {:position "absolute"
   :z-index 2
   :top 0
   :right 0
   :width "360px"
   :height "100%"
   :display "flex"
   :justify-content "space-between"
   :align-items "center"
   :backdrop-filter "blur(8px)"
   })

(def pagemark-card-style
  {:width "250px"
  ;;  :margin "0 2rem"
   :margin-left "2.5rem"
   :pointer-events "auto"
   :border-radius "0.25rem"
   :background (color :background-plus-1-color :opacity-high)
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
                                :border "2px solid red"}}
                           })

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
  (let [pagemarks (subscribe [:pagemark/sidebar])]
    (fn []
      [:div#createPagemark (use-style pagemark-style)
        ;; (into [:div] (map #(do ^{:key (:key %)} [pagemark-card %]) @pagemarks))]
       [pagemark-card]
       [:div {:style {:position "relative"
                      :z-index 1
                      :top 1
                      :right 1
                      :bottom 1
                      :height "100%"
                      ;; :width scrollbar-width
                      ;; :width "68px"
                      :width "68px"
                      ;; :background "transparent"
                      ;; :backdrop-filter "blur(8px)"
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

(defn scrollbar
  [content]
  (let [state (r/atom {:container nil
                       :top 0
                       :scroll-height 0
                       :window-height 0
                       :thumb-height 0
                       :scaling nil
                       :bottom-limit nil
                       :pagemark? false})
        new-pos (fn [y]
                  (let [new-y  (cond
                                 (< y 0) 0
                                 (> y (:bottom-limit @state)) (:bottom-limit @state)
                                 :else y)]
                    (.scroll (:container @state) 0 (* (:scaling @state) new-y))
                    (swap! state assoc :top new-y)))
        pointer-move-handler #(new-pos (+ (:top @state) (.-movementY %)))
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
                                 2 (do
                                     (swap! state update :pagemark? not)
                                     (.addEventListener js/document
                                                        "pointerdown"
                                                        (fn close-pagemark [e]
                                                          (when (nil? (.closest (.-target e) "#createPagemark"))
                                                            (.removeEventListener js/document "pointerdown" close-pagemark)
                                                            (swap! state update :pagemark? not)))))))
        scroll-handler (fn [e]
                         (->> (/ (.. e -target -scrollTop) (:scroll-height @state))
                              (* (:window-height @state))
                              (swap! state assoc :top)))
        resize-observer (js/ResizeObserver. (fn [e]
                                              (let [window-changed (.find e #(= "scrollbar" (.. % -target -id)))
                                                    scroll-changed (.find e #(= "viewer" (.. % -target -id)))
                                                    window-height (if (some? window-changed)
                                                                    (.. window-changed -contentRect -height)
                                                                    (:window-height @state))
                                                    scroll-height (if (some? scroll-changed)
                                                                    (.. scroll-changed -contentRect -height)
                                                                    (:scroll-height @state))
                                                    bottom-limit (->> (/ window-height scroll-height)
                                                                      (- 1)
                                                                      (* window-height))
                                                    scaling      (/ scroll-height window-height)
                                                    thumb-height (* window-height (/ window-height scroll-height))
                                                    top (* (:top @state) (:scaling @state) (/ scaling))]
                                                (reset! state {:container (:container @state)
                                                               :top top
                                                               :scroll-height scroll-height
                                                               :window-height window-height
                                                               :thumb-height thumb-height
                                                               :scaling scaling
                                                               :bottom-limit bottom-limit
                                                               :pagemark? (:pagemark? @state)}))))]
    (r/create-class
     {:display-name "scrollbar-wrapper"
      :component-did-mount (fn []
                             (->> (.getElementById js/document "viewerContainer")
                                  (swap! state assoc :container))
                             (.observe resize-observer (.getElementById js/document "viewer"))
                             (.observe resize-observer (.getElementById js/document "scrollbar")))
      :conponent-will-unmount #(.disconnect resize-observer)
      :reagent-render (fn []
                        [:div#scrollWrapper (merge (use-style scroll-container-style)
                                                   {:on-scroll scroll-handler
                                                    :on-context-menu #(.preventDefault %)})
                         content
                         (when (:pagemark? @state)
                           [pagemark])
                         [:div#scrollbar.scrollbar (merge (use-sub-style scroll-container-style :scrollbar)
                                                          {:on-pointer-down pointer-down-handler})
                          [:div.thumb (merge (use-sub-style scroll-container-style :thumb)
                                             {:style {:top (:top @state)
                                                      :height (str (:thumb-height @state) "px")}})]
                          [:div {:style {:position "absolute"
                                         :background "green"
                                         :opacity 0.4
                                         :width "30%"
                                         :height "300px"
                                         :top "200px"}}]
                          [:div {:style {:position "absolute"
                                         :background "red"
                                         :opacity 0.4
                                         :width "30%"
                                         :height "100px"
                                         :top "0px"}}]
                          [:div {:style {:position "absolute"
                                         :background "blue"
                                         :opacity 0.4
                                         :width "30%"
                                         :height "200px"
                                         :top "600px"}}]
                          [:div {:style {:position "absolute"
                                         :background "green"
                                         :opacity 0.4
                                         :width "30%"
                                         :height "400px"
                                         :top "900px"}}]]])})))

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