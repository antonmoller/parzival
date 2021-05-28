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
   :scrollbar-width "none"
  ;;  ::stylefy/mode {"::-webkit-scrollbar" {;:background "rgba(121, 121, 121, 0.4)"
  ;;  ::stylefy/mode {"::-webkit-scrollbar" {;:background "rgba(121, 121, 121, 0.4)"
  ;;                                         :border "1px solid rgb(69, 69, 69)"
  ;;                         ;; :shadow "#000000"
  ;;                                         ;; :box-shadow (str (:64 DEPTH-SHADOWS) ", 0 0 0 1px " (color :body-text-color :opacity-lower))
  ;;                         ;; :border-right "0.2px solid rgb(191, 191, 191)"
  ;;                                         :width "2.5rem"}
  ;;                  "::-webkit-scrollbar-thumb" {
  ;;                                               :background "rgba(191, 191, 191, 0.4)"
  ;;                                               ;; :background "none"
  ;;                                               ;; :box-shadow (str (:64 DEPTH-SHADOWS) ", 0 0 0 1px " "rgba(191,191,191,0.4)");(color :body-text-color :opacity-lower))
  ;;                                               :box-shadow "0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19)"
        
                                                ;; }}
  ;; ::stylefy/mode [["::-webkit-scrollbar" {;:background-color "blue"
  ;;                                         ;;  :border-left "1px solid grey"
  ;;                                         ;;  :border-right "1px solid grey"
  ;;                                         ;;  :width "40px"
  ;;                                         }]
  ;;                  ["::-webkit-scrollbar-thumb" {:background "#000"
  ;;                                               ;;  :opacity 0.4
  ;;                                               ;;  :border-left "1px solid grey"
  ;;                                               ;;  :border-right "1px solid grey"
  ;;                                                ; :height "10px"
  ;;                                                }]]
})

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