(ns parzival.views.pdf.pagemark-sidebar
  (:require
   ["@material-ui/icons/Close" :default Close]
   ["@material-ui/icons/Done" :default Done]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [parzival.utils :as utils]
   [parzival.style :refer [DEPTH-SHADOWS PAGEMARK-COLOR color PDF-SCROLLBAR-WIDTH]]
   [parzival.views.buttons :refer [button]]
   [stylefy.core :as stylefy :refer [use-style use-sub-style]]))

(def WIDTH
  (-> (js/parseInt PDF-SCROLLBAR-WIDTH) ; Don't count left/right borders
      (- 2)
      (str "px")))

;;; Styles

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
                                            :width (-> (js/parseFloat WIDTH) (+ 17) (str "px"))
                                            :pointer-events "auto"
                                            ::stylefy/mode {:before {:content "''"
                                                                     :position "absolute"
                                                                     :top "-1px"
                                                                     :bottom "-1px"
                                                                     :width "16px"
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
   :border-radius "0.25rem 0 0 0.25rem"
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

;;; Components

(defn pagemark
  [{:keys [type top height style handle-click]}]
  [:div (merge (use-sub-style pagemark-style style)
               {:on-click handle-click
                :style {:top top
                        :height height
                        :background (type PAGEMARK-COLOR)
                        :cursor (cond
                                  (and (= :edit style) (= :done type)) "not-allowed"
                                  (= :edit style) "pointer"
                                  :else "default")}})])

(defn pagemark-change
  [pagemarks num-pages page-quota]
  (let [state (r/atom {:selected nil
                       :action nil
                       :create? false
                       :before-edit nil 
                       :start-page ""
                       :end-page ""
                       :deadline ""
                       :low-lim 1
                       :high-lim num-pages})
        today (as-> (js/Date.) date
                (str (.getFullYear date) "-"
                     (.padStart (str (inc (.getMonth date))) 2 "0") "-"
                     (.padStart (str (.getDate date)) 2 "0")))
        page-change (fn [e page]
                      (swap! state assoc page (int (.. e -target -value)))
                      (set! (.. (:selected @state) -style -top) 
                            (utils/top-percentage {:start-page (:start-page @state)} page-quota))
                      (set! (.. (:selected @state) -style -height) 
                            (utils/height-percentage {:start-page (:start-page @state) 
                                                      :end-page (:end-page @state) 
                                                      :end-area 1}
                                                     page-quota)))
        valid-click? (fn [page] (not-any? #(<= (:start-page %) page (:end-page %)) @pagemarks))
        low-limit #(-> (some (fn [p] (when (< (:end-page p) %) (:end-page p))) @pagemarks) (inc))
        high-limit #(let [page (some (fn [p] (when (> (:start-page p) %) (:start-page p)))
                                     (reverse @pagemarks))]
                      (if (some? page) (dec page) num-pages))
        reset-css #(let [selected (:selected @state)
                         {:keys [top height color]} (:before-edit @state)]
                     (when (some? selected)
                       (swap! state assoc :selected nil :create? false :before-edit nil :deadline "" :start-page nil :end-page nil)
                       (set! (.-style selected) (str "width:" WIDTH "; top:" top "; height:" height
                                                     "; background:" color "; cursor:" 
                                                     (.. selected -style -cursor) ";"))))
        handle-date-change (fn [e]
                             (swap! state assoc :deadline (.. e -target -value))
                             (set! (.. (:selected @state) -style -background)
                                   (if (empty? (.. e -target -value))
                                     (:skip PAGEMARK-COLOR)
                                     (:deadline PAGEMARK-COLOR))))
        handle-click-pagemark (fn [e {:keys [type deadline start-page end-page] :as v}]
                                (.stopPropagation e)
                                (reset-css)
                                (when (not= :done type)
                                  (set! (.. e -target -style -width) "100%")
                                  (swap! state assoc
                                         :selected (.. e -target)
                                         :before-edit (assoc v :color (type PAGEMARK-COLOR)) 
                                         :start-page start-page
                                         :end-page end-page
                                         :deadline deadline
                                         :low-lim (low-limit start-page)
                                         :high-lim (high-limit end-page))))
        handle-click-scrollbar (fn [e]
                                 (.stopPropagation e)
                                 (reset-css)
                                 (let [page (as-> (.getComputedStyle js/window (.-target e)) x
                                              (js/parseFloat (.-height x))
                                              (/ (.-clientY e) x)
                                              (-> x (/ page-quota) (int) (inc)))]
                                   (when (valid-click? page)
                                     (let [pagemark (.getElementById js/document "pagemark-tmp")
                                           top (utils/top-percentage {:start-page page} page-quota)
                                           height (utils/height-percentage {:start-page page 
                                                                            :end-page page 
                                                                            :end-area 1} 
                                                                           page-quota)]
                                       (set! (.. pagemark -style -top) top)
                                       (set! (.. pagemark -style -height) height)
                                       (set! (.. pagemark -style -width) "100%")
                                       (swap! state assoc
                                              :selected pagemark
                                              :before-edit {:top top :height height :color (:skip PAGEMARK-COLOR)}
                                              :create? true
                                              :start-page page
                                              :end-page page
                                              :low-lim (low-limit page)
                                              :high-lim (high-limit page))))))
        handle-submit (fn [e]
                        (.preventDefault e)
                        (case (:action @state)
                          :create (dispatch [:pagemark/sidebar-create
                                             (:start-page @state)
                                             (:end-page @state)
                                             (:deadline @state)])
                          :update (as-> (:before-edit @state) {:keys [start-page end-page]}
                                    (dispatch [:pagemark/sidebar-update
                                               start-page
                                               (:start-page @state)
                                               end-page
                                               (:end-page @state)
                                               (:deadline @state)]))
                          :delete (as-> (:before-edit @state) {:keys [start-page end-page]}
                                    (dispatch [:pagemark/sidebar-delete start-page end-page])))
                        (reset-css))]
    (fn []
      [:div#createPagemark (merge (use-style pagemark-style)
                                  {:on-pointer-down #(.stopPropagation %)})
       [:form (use-style pagemark-card-style
                         {:on-submit handle-submit})
        [:label (use-sub-style pagemark-card-style :label) "Pages"]
        [:input (use-sub-style pagemark-card-style :input
                               {:type "number"
                                :disabled (nil? (:selected @state))
                                :required true
                                :value (:start-page @state)
                                :min (:low-lim @state)
                                :max (:end-page @state)
                                :on-change #(page-change % :start-page)})]
        [:input (use-sub-style pagemark-card-style :input
                               {:type "number"
                                :disabled (nil? (:selected @state))
                                :required true
                                :value (:end-page @state)
                                :min (:start-page @state)
                                :max (:high-lim @state)
                                :on-change #(page-change % :end-page)})]
        [:label (use-sub-style pagemark-card-style :label) "Deadline"]
        [:input (use-sub-style pagemark-card-style :input
                               {:type "date"
                                :disabled (nil? (:selected @state))
                                :min today
                                :value (:deadline @state)
                                :on-change handle-date-change})]
        [:div (use-sub-style pagemark-card-style :row-container)
         [button {:type "submit"
                  :disabled (or (nil? (:selected @state))
                                (= "pagemark-tmp" (.getAttribute (:selected @state) "id")))
                  :on-click #(swap! state assoc :action :delete)}
          [:> Close]]
         [button {:type "submit"
                  :disabled (nil? (:selected @state))
                  :on-click #(swap! state assoc :action (if (:create? @state) :create :update))}
          [:> Done]]]]
       (into [:div (use-sub-style pagemark-style :change-container {:on-click handle-click-scrollbar})
              [:div (merge (use-sub-style pagemark-style :edit)
                           {:id "pagemark-tmp"
                            :on-click #(.stopPropagation %)
                            :style {:display (if (:create? @state) "block" "none")
                                    :width "100%"
                                    :cursor "pointer"
                                    :background (:skip PAGEMARK-COLOR)}})]]
             (map (fn [{:keys [type top height] :as v}]
                           [pagemark {:handle-click #(handle-click-pagemark % v)
                                      :type type
                                      :top top
                                      :height height
                                      :style :edit}])
                         @pagemarks))])))

(defn pagemark-sidebar
  []
  (let [sidebar-open?  @(subscribe [:pagemark/sidebar-open?])
        pagemarks  (subscribe [:pagemark/sidebar])
        num-pages  @(subscribe [:pdf/num-pages])
        page-quota @(subscribe [:pdf/page-quota])]
    (if sidebar-open?
      [pagemark-change pagemarks num-pages page-quota]
      (into [:div] (map (fn [{:keys [type top height]}]
                          [pagemark {:type type
                                     :top top
                                     :height height
                                     :style :sidebar}])
                        @pagemarks)))))