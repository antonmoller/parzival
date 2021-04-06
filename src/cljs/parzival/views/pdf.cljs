(ns parzival.views.pdf
  (:require
            [re-frame.core :refer [dispatch subscribe]]
            [goog.object :as obj]
            [parzival.style :refer [ZINDICES]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def pdf-container-style
  {:position "absolute"
   :z-index (:zindex-dropdown ZINDICES)
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
                                                 }]]
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
      [:div#viewerContainer (use-style pdf-container-style) ;TODO: Check if left mouse button is pressed
        [:div#viewer.pdfViewer {:on-mouse-up (fn [] (dispatch [:highlight "rgba(0,100,0,1)"
                                                              :id "testing"]))
                                :on-mouse-down (fn [e] (if (= (obj/get e "button") 2)
                                                        (dispatch [:pagemark])))}]])))

