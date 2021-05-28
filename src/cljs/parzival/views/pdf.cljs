(ns parzival.views.pdf
  (:require
            [re-frame.core :refer [dispatch subscribe]]
            [parzival.views.highlight-toolbar :refer [highlight-toolbar]]
            [parzival.views.pagemark-menu :refer [pagemark-menu]]
            [parzival.style :refer [ZINDICES DEPTH-SHADOWS color]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def pdf-container-style
  {:position "absolute"
   :z-index (:zindex-sticky ZINDICES) ; FIXME
   :height "100%"
   :overflow-y "auto"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode [
                   ["::-webkit-scrollbar-thumb" {:background "rgba(37,37,38,0.4)"}]
                   [:hover::-webkit-scrollbar-thumb {:background "rgba(121, 121, 121, 0.4)"}]
                   ["::-webkit-scrollbar-thumb:hover" {:background "rgba(100, 100, 100, 0.7)"}]
                   ["::-webkit-scrollbar-thumb:active" {:background "rgba(191, 191, 191, 0.4)"}]
                   ["::-webkit-scrollbar" {:width "40px"
                                          :border "1px solid rgb(69, 69, 69)"}]
                  ;;  ["::-webkit-scrollbar:shadow:top" {
                  ;;                                     :border "1px solid rgb(69, 69, 69)"}]
                   ]})

;;; Components

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
      [:div#viewerContainer (use-style pdf-container-style)
       [highlight-toolbar]
       [pagemark-menu]
       [:div#viewer.pdfViewer {:on-mouse-up #(dispatch [:highlight/toolbar-create])
                               :on-context-menu (fn [e]
                                                  (.preventDefault e)
                                                  (dispatch [:pagemark/menu (.-target e) (.-clientX e) (.-clientY e)]))}]])))