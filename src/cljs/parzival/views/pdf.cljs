(ns parzival.views.pdf
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [parzival.views.pagemark-sidebar :refer [pagemark-sidebar]]
   [parzival.views.highlight-toolbar :refer [highlight-toolbar]]
   [parzival.views.pagemark-menu :refer [pagemark-menu]]
   [parzival.views.virtual-scrollbar :refer [virtual-scrollbar]]
   [parzival.style :refer [PDF-SCROLLBAR-WIDTH]]
   [stylefy.core :as stylefy :refer [use-style]]))

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

;;; Components

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