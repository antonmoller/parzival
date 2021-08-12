(ns parzival.views.pdf
  (:require
   [reagent.core :as r]
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
   :width "100%"
   :overflow-x "hidden"
   :overflow-y "scroll"
   :scrollbar-width "none"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode {"::-webkit-scrollbar" {:display "none"}}})

;;; Components

; TODO subscribe to document and use (.setDocument when it changes)
; Start the viewer when the app launches
(defn pdf
  [display?]
  [:div (if display? {} {:style {:display "none"}})
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
     :container-width "1000px" ; The width of the container
     :scrollbar-content [pagemark-sidebar]
     :scrollbar-width PDF-SCROLLBAR-WIDTH}]])