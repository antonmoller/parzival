(ns parzival.views.pdf
  (:require
   ["@material-ui/icons/Close" :default Close]
   ["@material-ui/icons/ArrowRightAlt" :default ArrowRightAlt]
   ["@material-ui/icons/Add" :default Add]
   ["@material-ui/icons/Done" :default Done]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [parzival.views.highlight-toolbar :refer [highlight-toolbar]]
   [parzival.views.pagemark-menu :refer [pagemark-menu]]
   [parzival.views.buttons :refer [button]]
   [parzival.views.virtual-scrollbar :refer [virtual-scrollbar]]
   [parzival.style :refer [ZINDICES DEPTH-SHADOWS PAGEMARK-COLOR color PDF-SCROLLBAR-WIDTH]]
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
   :height "100%"
   :display "flex"
   :justify-content "space-between"
   :align-items "center"
   :pointer-events "none"})

(def pagemark-sidebar-style
  {:position "absolute"
   :width "30%"
   :opacity "0.4"})

(def pagemark-change-style
  {:position "relative"
   :z-index 1
   :top 1
   :right 1
   :bottom 1
   :height "100%"
   :width "68px"
  ;;  :border-left (str "1px solid " (color :background-plus-1-color))
   :pointer-events "auto"})

(def pagemark-edit-style
  {:position "absolute"
   :right 0
   :width (-> (js/parseInt PDF-SCROLLBAR-WIDTH) ; Don't count left/right borders
              (- 2)
              (str "px"))
   :box-sizing "border-box"
   :cursor "not-allowed"
   :opacity 0.4})
  
;;; Helpers

(defn pagemark-sidebar-key
  [{:keys [type top height]}]
  (str "pagemark-sidebar-" type "-" top "-" height))

;;; Components

(def pagemark-card-style
  {:width "200px"
   :padding "1rem"
   :border-radius "0.25rem"
   :box-sizing "border-box"
   :display "flex"
   :flex-direction "column"
   :justify-content "space-around"
   :pointer-events "auto"
   :background (color :background-plus-1-color :opacity-high)
   :backdrop-filter "blur(8px)"
   :box-shadow [[(:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower)]]
   ::stylefy/sub-styles {:input {:width "95%"
                                 :align-self "flex-end"
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
                                 :margin-bottom "0.5rem"
                                 :box-sizing "border-box"
                                 ::stylefy/mode {:focus {:outline "none"}
                                                 "::placeholder" {:color (color :body-text-color :opacity-med)}}}
                         :label {:font-weight "700"
                                 :font-size "0.75rem"
                                 :line-height "1.7"}
                         :row-container {:display "flex"
                                         :justify-content "space-between"}
                         :line {:height "125px"
                                :width "20px"
                                :border "2px solid red"}}})

(defn pagemark-card
  []
  (let [state (r/atom {:start-page ""
                       :end-page ""
                       :deadline ""})
        no-pages (subscribe [:pdf/no-pages])
        today (as-> (js/Date.) date
                (str (.getFullYear date) "-"
                     (.padStart (str (inc (.getMonth date))) 2 "0") "-"
                     (.padStart (str (.getDate date)) 2 "0")))]
    (fn []
      [:form (use-style pagemark-card-style
                        {:on-submit (fn [e]
                                      (js/console.log "submit form")
                                      (.preventDefault e)
                                      (dispatch [:pagemark/sidebar-add @state])
                                      (reset! state {:start-page "" :end-page "" :deadline ""}))})
       [:label (use-sub-style pagemark-card-style :label) "Pages"]
       [:input (use-sub-style pagemark-card-style :input
                              (as-> (if (and (not= (:end-page @state) "")
                                             (<= 1 (:end-page @state) @no-pages))
                                      (:end-page @state)
                                      @no-pages) max-val
                                {:type "number"
                                 :value (:start-page @state)
                                 :on-change #(swap! state assoc :start-page (.. % -target -value))
                                 :required true
                                 :min 1
                                 :max max-val
                                 :placeholder (str "Start (1 - " max-val ")")}))]
       [:input (use-sub-style pagemark-card-style :input
                              (as-> (if (and (not= (:start-page @state) "") \
                                             (<= 1 (:start-page @state) @no-pages))
                                      (:start-page @state)
                                      1) min-val
                                {:type "number"
                                 :value (:end-page @state)
                                 :on-change #(swap! state assoc :end-page (.. % -target -value))
                                 :required true
                                 :min min-val
                                 :max @no-pages
                                 :placeholder (str "End (" min-val " - " @no-pages ")")}))]

       [:label (use-sub-style pagemark-card-style :label) "Deadline"]
       [:input (use-sub-style pagemark-card-style :input
                              {:type "date"
                               :value (:deadline @state)
                               :on-change #(swap! state assoc :deadline (.. % -target -value))
                               :id "deadline-input"
                               :min today
                               :placeholder "Deadline"})]
       [:div (use-sub-style pagemark-card-style :row-container)
        ;; TODO Clears the fields and removes currently active pagemark
        [button {;:on-click #(dispatch [:pagemark/sidebar-remove (:key value)])
                 :type "button"
                ;;  :disabled true
                ;;  :style {:background "rgba(255,0,0,0.3)"}
                 }
         [:> Close]]
        [button {:type "submit"
                ;;  :primary true
                ;;  :disabled true
                ;;  :style {:background "rgba(0,255,0,0.3)"}
                 }
         [:> Done]]]])))

(defn pagemark-sidebar
  []
  (let [pagemarks (subscribe [:pagemarks])
        pagemark? (subscribe [:pagemark?])
        state (r/atom {:start-page ""
                       :end-page ""
                       :deadline ""})
        no-pages (subscribe [:pdf/no-pages])
        today (as-> (js/Date.) date
                (str (.getFullYear date) "-"
                     (.padStart (str (inc (.getMonth date))) 2 "0") "-"
                     (.padStart (str (.getDate date)) 2 "0")))
        reset-state #(reset! state {:start-page "" :end-page "" :deadline ""})]
    (fn []
      (if-not @pagemark?
        (into [:div]
              (map #(do
                      ^{:key (pagemark-sidebar-key %)}
                      [:div (merge (use-style pagemark-sidebar-style)
                                   {:style {:top (:top %)
                                            :height (:height %)
                                            :background ((:type %) PAGEMARK-COLOR)}})])
                   @pagemarks))
        [:div#createPagemark (merge (use-style pagemark-style)
                                    {:on-pointer-down #(.stopPropagation %)})
         [:form (use-style pagemark-card-style
                           {:on-submit (fn [e]
                                         (.preventDefault e)
                                         (dispatch [:pagemark/sidebar-add @state])
                                         (reset-state))})
          [:label (use-sub-style pagemark-card-style :label) "Pages"]
          [:input (use-sub-style pagemark-card-style :input
                                 (as-> (if (and (not= (:end-page @state) "")
                                                (<= 1 (:end-page @state) @no-pages))
                                         (:end-page @state)
                                         @no-pages) max-val
                                   {:type "number"
                                    :value (:start-page @state)
                                    :on-change #(swap! state assoc :start-page (.. % -target -value))
                                    :required true
                                    :min 1
                                    :max max-val
                                    :placeholder (str "Start (1 - " max-val ")")}))]
          [:input (use-sub-style pagemark-card-style :input
                                 (as-> (if (and (not= (:start-page @state) "") \
                                             (<= 1 (:start-page @state) @no-pages))
                                         (:start-page @state)
                                         1) min-val
                                   {:type "number"
                                    :value (:end-page @state)
                                    :on-change #(swap! state assoc :end-page (.. % -target -value))
                                    :required true
                                    :min min-val
                                    :max @no-pages
                                    :placeholder (str "End (" min-val " - " @no-pages ")")}))]

          [:label (use-sub-style pagemark-card-style :label) "Deadline"]
          [:input (use-sub-style pagemark-card-style :input
                                 {:type "date"
                                  :value (:deadline @state)
                                  :on-change #(swap! state assoc :deadline (.. % -target -value))
                                  :id "deadline-input"
                                  :min today
                                  :placeholder "Deadline"})]
          [:div (use-sub-style pagemark-card-style :row-container)
        ;; TODO Clears the fields and removes currently active pagemark
           [button {:on-click (fn [_]
                                (reset-state)
                                )
                    ;#(dispatch [:pagemark/sidebar-remove (:key value)])
                    :type "button"
                ;;  :disabled true
                ;;  :style {:background "rgba(255,0,0,0.3)"}
                    }
            [:> Close]]
           [button {:type "submit"
                ;;  :primary true
                ;;  :disabled true
                ;;  :style {:background "rgba(0,255,0,0.3)"}
                    }
            [:> Done]]]]
         (into [:div (use-style pagemark-change-style)]
               (map #(do
                       ^{:key (pagemark-sidebar-key %)}
                       [:div (merge (use-style pagemark-edit-style)
                                    {:on-click (fn [e]
                                                 (set! (.. e -target -style -width) "100%")
                                                 (reset! state {:start-page (str (js/parseInt (:top %)))
                                                                :end-page (str (js/parseInt (:height %)))
                                                                :deadline "2020-12-20"}))
                                     :style {:top (:top %)
                                             :height (:height %)
                                             :cursor (if (not= :done (:type %))
                                                       "pointer"
                                                       "not-allowed")
                                             :background ((:type %) PAGEMARK-COLOR)
                                             :border-top (str "1px solid " ((:type %) PAGEMARK-COLOR))
                                             :border-bottom (str "1px solid " ((:type %) PAGEMARK-COLOR))}})])
                    @pagemarks))]))))

(defn pdf
  []
  (let [pdf? (subscribe [:pdf?])
        loading? (subscribe [:pdf/loading?])
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
        :scroll-container-id "viewerContainer" ; the container where the scrollbar would be
        :container-id "viewer" ; The container that contains the content that will be scrolled
        :container-width "855px" ; The width of the container
        :scrollbar-content [pagemark-sidebar]
        :scrollbar-width PDF-SCROLLBAR-WIDTH}])))