(ns parzival.views.pdf.pdf
  (:require
   [re-frame.core :refer [dispatch]]
   [parzival.views.pdf.pagemark-sidebar :refer [pagemark-sidebar]]
   [parzival.views.pdf.highlight-toolbar :refer [highlight-toolbar]]
   [parzival.views.pdf.pdf-menu :refer [pdf-menu]]
   [parzival.views.virtual-scrollbar :refer [virtual-scrollbar]]
   [parzival.style :refer [PDF-SCROLLBAR-WIDTH]]
   [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def pdf-container-style
  {:position "absolute"
   :height "100%"
   :width "100%"
   :overflow-x "hidden"
   :overflow-y "scroll"
   :scrollbar-width "none"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode {"::-webkit-scrollbar" {:display "none"}}})

;;; Components

(defn pdf
  [display?]
  [:div (if display? {} {:style {:display "none"}})
   [virtual-scrollbar
    {:content  [:div#viewerContainer (use-style pdf-container-style)
                [highlight-toolbar]
                [pdf-menu]
                [:div#viewer.pdfViewer {:on-mouse-up #(dispatch [:highlight/toolbar-create])
                                        :on-context-menu (fn [e]
                                                           (.preventDefault e)
                                                           (dispatch [:pdf/menu-open
                                                                      (.-target e)
                                                                      (.-clientX e)
                                                                      (.-clientY e)]))}]]
     :scroll-container-id "viewerContainer" ; the container where the scrollbar would be
     :container-id "viewer" ; The container that contains the content that will be scrolled
     :container-width "1000px" ; The width of the container
     :scrollbar-content [pagemark-sidebar]
     :scrollbar-width PDF-SCROLLBAR-WIDTH}]])