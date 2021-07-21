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


(def WIDTH
  (-> (js/parseInt PDF-SCROLLBAR-WIDTH) ; Don't count left/right borders
      (- 2)
      (str "px")))


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
   :pointer-events "none"
   ::stylefy/sub-styles {:change-container {:position "relative"
                                            :z-index 1
                                            :top 1
                                            :right 1
                                            :bottom 1
                                            :height "100%"
                                            :width "68px"
                                            :pointer-events "auto"
                                            ::stylefy/mode {:before {:content "''"
                                                                     :position "absolute"
                                                                     :top "-1px"
                                                                     :bottom "-1px"
                                                                     :width (-> (- 68 (js/parseInt WIDTH))
                                                                                (str "px"))
                                                                     :background (color :background-plus-1-color :opacity-high)
                                                                     :backdrop-filter "blur(8px)"}}}
                         :sidebar {:position "absolute"
                                   :width "30%"
                                   :opacity "0.4"}
                         :edit {:position "absolute"
                                :right 0
                                :width WIDTH
                                :box-sizing "border-box"
                                :cursor "not-allowed"
                                :opacity "0.4"}}})

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
                                         :justify-content "space-between"}}})

;;; Helpers

(defn pagemark-sidebar-key
  [{:keys [start-page end-page]}]
  (str "pagemark-scrollbar-" start-page "-" end-page))

(defn calc-top
  [{:keys [start-page page-percentage]}]
  (-> (* 100 page-percentage (dec start-page)) (str "%")))

(defn calc-height
  ([{:keys [start-page end-page end-area page-percentage]}]
   (-> (- end-page start-page)
       (+ end-area)
       (* 100 page-percentage)
       (str "%"))))

(defn today
  []
  (as-> (js/Date.) date
    (str (.getFullYear date) "-"
         (.padStart (str (inc (.getMonth date))) 2 "0") "-"
         (.padStart (str (.getDate date)) 2 "0"))))

(defn calc-limits
  [start-page end-page no-pages pagemarks]
  (reduce (fn [m v]
            (cond-> m
              (< (:low-limit m) (:end-page v) (int start-page)) (assoc
                                                                 :low-limit
                                                                 (inc (:end-page v)))
              (< (int end-page) (:start-page v)) (assoc
                                                  :high-limit
                                                  (dec (:start-page v)))))
          {:low-limit 1 :high-limit no-pages}
          pagemarks))

(defn valid-click
  [e no-pages pagemarks]
  (let [click-pos (->> (.-target e)
                       (.getComputedStyle js/window)
                       (.-height)
                       (js/parseFloat)
                       (/ (.-clientY e)))
        page (inc (int (/ click-pos (/ no-pages))))]
    (when (every? (fn [{:keys [start-page end-page]}]
                    (not (<= start-page page end-page)))
                  pagemarks)
      (let [style (.-style (.getElementById js/document "pagemark-tmp"))
            {:keys [low-limit high-limit]} (calc-limits page page no-pages pagemarks)]
        [page style low-limit high-limit]))))

(defn set-color
  [value style]
  (let [color (if (= "" value)
                (:skip PAGEMARK-COLOR)
                (:schedule PAGEMARK-COLOR))]
    (set! (.-background style) color)
    (set! (.-borderTop style) (str "1px solid " color))
    (set! (.-borderBottom style) (str "1px solid " color))))

;;; Components

(defn pagemark
  [{:keys [id on-click style edit?] :as v}]
  [:div (merge (use-sub-style pagemark-style style)
               {:id id
                :on-click on-click
                :style {:top (calc-top v)
                        :height (calc-height v)
                        :border-top (when edit?
                                      (str "1px solid " ((:type v) PAGEMARK-COLOR)))
                        :border-bottom (when edit?
                                         (str "1px solid " ((:type v) PAGEMARK-COLOR)))
                        :background ((:type v) PAGEMARK-COLOR)
                        :cursor (cond
                                  (not edit?) "default"
                                  (= :done (:type v)) "not-allowed"
                                  :else "pointer")}})])

(defn pagemark-change
  [pagemarks no-pages page-percentage]
  (let [state (r/atom {:start-page ""
                       :end-page ""
                       :edit-start ""
                       :edit-end ""
                       :deadline ""
                       :style nil
                       :low-limit 1
                       :high-limit @no-pages
                       :adding? false})
        reset-state #(reset! state {:start-page "" :end-page "" :edit-start "" :edit-end "" :deadline "" 
                                    :style nil :low-limit 1 :high-limit @no-pages :adding? false})
        reset-css #(when (some? (:style @state))
                     (set! (.-width (:style @state)) WIDTH)
                     (set! (.-top (:style @state))
                           (calc-top {:start-page (:edit-start @state)
                                      :page-percentage page-percentage}))
                     (set! (.-height (:style @state))
                           (calc-height {:start-page (:edit-start @state)
                                         :end-page (:edit-end @state)
                                         :end-area 1
                                         :page-percentage page-percentage})))
        handle-click (fn [style {:keys [type start-page end-page schedule]}]
                       (when (not= :done type)
                         (let [{:keys [low-limit high-limit]} (calc-limits start-page end-page
                                                                           @no-pages @pagemarks)]
                           (reset-css)
                           (set! (.-width style) "100%")
                           (reset! state {:start-page start-page
                                          :end-page end-page
                                          :edit-start start-page
                                          :edit-end end-page
                                          :deadline schedule
                                          :style style
                                          :low-limit low-limit
                                          :high-limit high-limit
                                          :adding? false}))))
        handle-click-scrollbar (fn [e]
                                 (when-let [[page style low-limit high-limit] (valid-click e @no-pages @pagemarks)]
                                   (reset-css)
                                   (set-color "" style)
                                   (set! (.-top style) (calc-top {:start-page page
                                                                  :page-percentage page-percentage}))
                                   (set! (.-width style) "100%")
                                   (set! (.-height style) (-> (* 100 page-percentage) (str "%")))
                                   (reset! state {:start-page page
                                                  :end-page page
                                                  :edit-start page
                                                  :edit-end page
                                                  :deadline ""
                                                  :style style
                                                  :low-limit low-limit
                                                  :high-limit high-limit
                                                  :adding? true})))
        handle-change (fn [value page]
                        (when (some? (:style @state))
                          (set! (.-background (:style @state)) (if (= "" (:deadline @state))
                                                                 (:skip PAGEMARK-COLOR)
                                                                 (:schedule PAGEMARK-COLOR)))
                          (set! (.-height (:style @state))
                                (calc-height {:start-page (if (= :start-page page)
                                                            value
                                                            (:start-page @state))
                                              :end-page (if (= :end-page page)
                                                          value
                                                          (:end-page @state))
                                              :end-area 1
                                              :page-percentage page-percentage}))
                          (when (= :start-page page)
                            (set! (.-top (:style @state))
                                  (calc-top {:start-page value
                                             :page-percentage page-percentage}))))
                        (swap! state assoc page value))
        handle-date-change (fn [value]
                             (set-color value (:style @state))
                             (swap! state assoc :deadline value))
        handle-delete (fn []
                        (dispatch [:pagemark/sidebar-remove (:start-page @state) (:end-page @state)])
                        (reset-state))
        handle-submit (fn [e]
                        (.preventDefault e)
                        (dispatch [:pagemark/sidebar-add-edit @state])
                        (reset-css)
                        (reset-state))]
    (fn []
      [:div#createPagemark (merge (use-style pagemark-style)
                                  {:on-pointer-down #(.stopPropagation %)})
       [:form (use-style pagemark-card-style
                         {:on-submit handle-submit})
        [:label (use-sub-style pagemark-card-style :label) "Pages"]
        [:input (use-sub-style pagemark-card-style :input
                               {:type "number"
                                :value (:start-page @state)
                                :min (:low-limit @state)
                                :max (:end-page @state)
                                :on-change #(handle-change (.. % -target -value) :start-page)
                                :disabled (nil? (:style @state))
                                :required true})]
        [:input (use-sub-style pagemark-card-style :input
                               {:type "number"
                                :value (:end-page @state)
                                :min (:start-page @state)
                                :max (:high-limit @state)
                                :on-change #(handle-change (.. % -target -value)  :end-page)
                                :disabled (nil? (:style @state))
                                :required true})]
        [:label (use-sub-style pagemark-card-style :label) "Deadline"]
        [:input (use-sub-style pagemark-card-style :input
                               {:type "date"
                                :min (today)
                                :value (:deadline @state)
                                :on-change #(handle-date-change (.. % -target -value))
                                :disabled (nil? (:style @state))})]
        [:div (use-sub-style pagemark-card-style :row-container)
         [button {:on-click handle-delete
                  :type "button"
                  :disabled (or (nil? (:style @state))
                                (:adding? @state)
                                (not= (int (:edit-start @state)) (int (:start-page @state)))
                                (not= (int (:edit-end @state)) (int (:end-page @state))))}
          [:> Close]]
         [button {:type "submit"
                  :disabled (nil? (:style @state))}
          [:> Done]]]]
       (into [:div (use-sub-style pagemark-style :change-container
                                  {:on-click handle-click-scrollbar})
              [:div (merge (use-sub-style pagemark-style :edit)
                           {:id "pagemark-tmp"
                            :style {:visibility (if (:adding? @state)
                                                  "visible"
                                                  "hidden")
                                    :width "100%"
                                    :cursor "pointer"
                                    :background (:skip PAGEMARK-COLOR)}})]]
             (map (fn [v]
                    (as-> (pagemark-sidebar-key v) key
                      ^{:key key}
                      [pagemark (merge {:id key
                                        :on-click (fn [e]
                                                    (.stopPropagation e)
                                                    (handle-click (.. e -target -style) v))
                                        :style :edit
                                        :edit? true
                                        :page-percentage page-percentage}
                                       (if (= :done (:type v))
                                         (assoc v :end-area 1)
                                         v))]))
                  @pagemarks))])))

(defn pagemark-sidebar
  []
  (let [pagemarks (subscribe [:pdf/pagemarks-sidebar])
        pagemark? (subscribe [:pagemark?])
        no-pages (subscribe [:pdf/no-pages])
        page-percentage (/ @no-pages)]
    (fn []
      (if @pagemark?
        [pagemark-change pagemarks no-pages page-percentage]
        (into [:div]
              (map (fn [v]
                     (as-> (pagemark-sidebar-key v) key
                       ^{:key key}
                       [pagemark (merge {:style :sidebar
                                         :edit? false
                                         :page-percentage page-percentage}
                                        v)]))
                   @pagemarks))))))

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