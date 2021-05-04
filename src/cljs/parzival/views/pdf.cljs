(ns parzival.views.pdf
  (:require
            [re-frame.core :refer [dispatch subscribe]]
            [parzival.views.highlight-toolbar :refer [highlight-toolbar]]
            [parzival.style :refer [ZINDICES]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def pdf-container-style
  {:position "absolute"
   :z-index (:zindex-sticky ZINDICES) ; FIXME
   :height "100%"
   :overflow-y "auto"
   ::stylefy/vendors ["webkit"]
   ::stylefy/mode [["::-webkit-scrollbar" {;:background-color "none"
                                           :border-left "1px solid grey"
                                           :border-right "1px solid grey"
                                           :width "40px"}]
                   ["::-webkit-scrollbar-thumb" {:background-color "blue"
                                                 :border-left "1px solid grey"
                                                 :border-right "1px solid grey"
                                                 ; :height "10px"
                                                 }]]})

;;; Components

;;; TODO: the on-click should only be active when the toolbar is not open
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
      [:div#viewerContainer (use-style pdf-container-style) ;TODO: Check if left mouse button is pressed
       [highlight-toolbar]
       [:div#viewer.pdfViewer {:on-mouse-up #(dispatch [:highlight/toolbar (.. % -target -parentNode)])
                               :on-context-menu (fn [e]
                                                  (.preventDefault e)
                                                  (dispatch [:pagemark (.-target e)])
                                                  ;; (.persist e)
                                                  ;; (js/console.log e)
                                                  )
                               }]])))

