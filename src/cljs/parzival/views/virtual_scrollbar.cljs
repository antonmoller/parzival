(ns parzival.views.virtual-scrollbar
  (:require
   [parzival.style :refer [ZINDICES SCROLLBAR]]
   [reagent.core :as r]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

;;; Styles

(def scroll-container-style
  {:position "relative"
   :z-index (:zindex-sticky ZINDICES)
   :width "0px"
   :height "100%"
   ::stylefy/sub-styles {:content {:position "absolute"
                                   :inset 0
                                   :margin "0 1rem"}
                         :scrollbar {:position "absolute"
                                     :right 0
                                     :height "100%"
                                     :box-sizing "border-box"
                                     :width "0px"
                                     :border (:border SCROLLBAR)
                                     :box-shadow (:shadow SCROLLBAR)}
                         :thumb {:position "absolute"
                                 :z-index 1
                                 :left 0
                                 :top 0
                                 :height "0px"
                                 :width "100%"
                                 :box-shadow (:thumb-shadow SCROLLBAR)
                                 :transition "background 0.8s linear"}}
   ::stylefy/manual [[:&:hover [:.thumb {:background (:thumb-visible-color SCROLLBAR)
                                         :box-shadow (:thumb-shadow SCROLLBAR)
                                         :transition "background 0.2s linear"}]]
                     [:.thumb:hover {:background (:thumb-hover-color SCROLLBAR)
                                     :box-shadow (:thumb-shadow SCROLLBAR)}]
                     [:.thumb:active {:background (:thumb-active-color SCROLLBAR)
                                      :box-shadow (:thumb-shadow SCROLLBAR)}]]})

;;; Components

(defn virtual-scrollbar
  "Usage: hide the scrollbar in the container using :scrollbar-width \"none\" and
   ::-webkit-scrollbar {:display \"none\"}. Set padding-right of the container to
   width that will fit the virtual scrollbar. Also set :overflow-x \"hidden\""
  [{:keys [content scroll-container-id container-id container-width scrollbar-content scrollbar-width]}]
  (let [state (r/atom {:container nil
                       :top 0
                       :scroll-height 0
                       :window-height 0
                       :thumb-height 0
                       :scaling 0
                       :bottom-limit 0})
        new-pos (fn [y]
                  (let [new-y  (cond
                                 (< y 0) 0
                                 (> y (:bottom-limit @state)) (:bottom-limit @state)
                                 :else y)]
                    (.scroll (:container @state) 0 (* (:scaling @state) new-y))
                    (swap! state assoc :top new-y)))
        pointer-move-handler #(new-pos (+ (:top @state) (.-movementY %)))
        scroll-handler (fn [e]
                         (->> (/ (.. e -target -scrollTop) (:scroll-height @state))
                              (* (:window-height @state))
                              (swap! state assoc :top)))
        pointer-up-handler (fn [e]
                             (.addEventListener (:container @state) "scroll" scroll-handler)
                             (doto (.-target e)
                               (.removeEventListener "pointermove" pointer-move-handler)
                               (.releasePointerCapture (.-pointerId e))))
        pointer-down-handler (fn [e]
                               (when (= 1 (.-buttons e))
                                 (if (.contains (.. e -target -classList) "thumb")
                                   (do
                                     (.removeEventListener (:container @state) "scroll" scroll-handler)
                                     (doto (.-target e)
                                       (.addEventListener "pointermove" pointer-move-handler)
                                       (.addEventListener "pointerup" pointer-up-handler (js-obj "once" true))
                                       (.setPointerCapture (.-pointerId e))))
                                   (new-pos (.-clientY e)))))
        resize-observer (js/ResizeObserver.
                         (fn [e]
                           (let [window-changed (.find e #(= "scrollbar" (.. % -target -id)))
                                 scroll-changed (.find e #(=  container-id (.. % -target -id)))
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
                                 thumb-height (* window-height
                                                 (/ window-height scroll-height))
                                 top (if (= scaling 0)
                                       0
                                       (* (:top @state) (:scaling @state) (/ scaling)))]
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
                             (let [scrollbar-height (.-clientHeight (.getElementById js/document "scrollbar"))]
                               (swap! state assoc :scroll-height scrollbar-height) ;; Will make inital thumb-height 100%
                               (swap! state assoc :window-height scrollbar-height) ;;
                               (doto (.getElementById js/document scroll-container-id)
                                 (.addEventListener "scroll" scroll-handler)
                                 (->> (swap! state assoc :container)))
                               (.observe resize-observer (.getElementById js/document container-id))
                               (.observe resize-observer (.getElementById js/document "scrollbar"))))
      :conponent-will-unmount #(.disconnect resize-observer)
      :reagent-render (fn []
                        [:div#scrollWrapper (merge (use-style scroll-container-style)
                                                   {:style {:width container-width}
                                                    :on-context-menu #(.preventDefault %)})
                         [:div (use-sub-style scroll-container-style :content)
                          content]
                         [:div#scrollbar.scrollbar (merge (use-sub-style scroll-container-style :scrollbar)
                                                          {:style {:width scrollbar-width}
                                                           :on-pointer-down pointer-down-handler})
                          [:div.thumb (merge (use-sub-style scroll-container-style :thumb)
                                             {:style {:top (:top @state)
                                                      :height (str (:thumb-height @state) "px")}})]
                          scrollbar-content]])})))