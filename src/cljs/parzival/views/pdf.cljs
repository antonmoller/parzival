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
                                 :z-index 1
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

(def pagemark-style
  {:position "absolute"
   :z-index 2
   :top 0
   :right 0
   :height "100%"
   :width "400px"
  ;;  :border "2px solid orange"
   ::stylefy/sub-styles {:viewer-overlay {:position "absolute"
                                          :top 0
                                          :left 0
                                          :width "360px"
                                          :height "100%"
                                          :background "transparent"
                                          :backdrop-filter "blur(8px)"
                                          :border-left "1px solid gray"
                                          :overflow-y "auto"
                                          :scrollbar-width "none"
                                          :display "flex"
                                          :flex-direction "column"
                                          :align-items "center"
                                          ::stylefy/vendors ["webkit"]
                                          ::stylefy/mode {"::-webkit-scrollbar" {:display "none"}}}
                         :scrollbar-overlay {:position "absolute"
                                             :top 0
                                             :right 0
                                             :width "32px"
                                             :height "100%"
                                             :background "transparent"}}})

(def pagemark-card-style
  {:width "250px"
   :margin "1rem 0"
   :border-radius "0.25rem"
   :background (color :background-plus-1-color :opacity-high)
   :box-shadow [[(:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower)]]
  ;;  :border "3px solid green"
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
                                                 "::placeholder" {:color (color :body-text-color :opacity-med)}}}}})

;;; Components

(defn pagemark-card
  [key value type]
  (case type
    :schedule [:div (use-style pagemark-card-style)
               [:div (use-sub-style pagemark-card-style :content)
                [button {:on-click #(dispatch [:pagemark/sidebar-remove key])
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
                                              {:placeholder "date"})
                               {:style {:width "160px"}})]]]
    :skip [:div (use-style pagemark-card-style)
           [:div (merge (use-sub-style pagemark-card-style :content)
                        {:style {:height "75px"}})
            [button {:on-click #(dispatch [:pagemark/sidebar-remove key])
                     :style {:align-self "flex-start"}}
             [:> Close]]
            [:div {:style {:display "flex"
                           :justify-content "center"
                           :align-items "center"}}
             [:input (use-sub-style pagemark-card-style :input
                                    {:placeholder "start"})]
             [:> ArrowRightAlt {:style {:font-size "2.375rem"}}]
             [:input (use-sub-style pagemark-card-style :input
                                    {:placeholder "end"})]]]]))

(defn pagemark
  []
  (let [pagemarks (subscribe [:pagemark/sidebar])]
    (fn []
      [:div#createPagemark (use-style pagemark-style)
       [:div (merge (use-sub-style pagemark-style :viewer-overlay)
                    {:on-scroll #(.stopPropagation %)})
        [:br]
        [button {:on-click #(dispatch [:pagemark/sidebar-add (random-uuid) {:start-page nil :end-page nil :deadline nil}])
                 :primary true
                 :style {;:background (PAGEMARK-COLOR :schedule)
                     ;:opacity (OPACITIES :opacity-high)
                     ;:color (color :link-color)
                         :width "250px"
                    ;;  :color "white"
                         }}
         [:<>
          [:span "Schedule Pages for Later"]
          [:> Add]]]
        [:br]
        [button {:on-click #(dispatch [:pagemark/sidebar-add (random-uuid) {:start-page nil :end-page nil}])
                 :primary true
                 :style {;:background "red"
                         :width "250px"
                         :color "red"}}
         [:<>
          [:span "Skip Pages"]
          [:> Add]]]
        (for [[k v] @pagemarks]
          (if (contains? v :deadline)
            ^{:key k} [pagemark-card k v :schedule]
            ^{:key k} [pagemark-card k v :skip]))]
       [:div (use-sub-style pagemark-style :scrollbar-overlay)]])))

(defn scrollbar
  [content]
  (let [scrollbar (subscribe [:pdf/scrollbar])
        state (r/atom {:top 0
                       :pagemark? false})
        new-pos (fn [y]
                  (let [new-y  (cond
                                 (< y 0) 0
                                 (> y (:bottom-limit @scrollbar)) (:bottom-limit @scrollbar)
                                 :else y)]
                    (.scroll (:container @scrollbar) 0 (* (:scaling @scrollbar) new-y))
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
                         (->> (/ (.. e -target -scrollTop) (:pdf-height @scrollbar))
                             (* (:track-height @scrollbar))
                             (swap! state assoc :top)))]
     (fn []
       [:div#scrollWrapper (merge (use-style scroll-container-style)
                                  {:on-scroll scroll-handler
                                   :on-context-menu #(.preventDefault %)})
        content
        (when (:pagemark? @state)
          [pagemark])
        [:div.scrollbar (merge (use-sub-style scroll-container-style :scrollbar)
                               {:on-pointer-down pointer-down-handler})
         [:div.thumb (merge (use-sub-style scroll-container-style :thumb)
                            {:style {:top (:top @state)
                                     :height (str (:thumb-height @scrollbar) "px")}})]
         [:div {:style {:position "absolute"
                        :background "blue"
                        :z-index 1
                        :opacity 0.3
                        :width "30%"
                        :height "200px"
                        :top "500px"}}]
         [:div {:style {:position "absolute"
                        :background "red"
                        :z-index 1
                        :opacity 0.3
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