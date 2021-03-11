(ns parzival.views.main-content
  (:require
            [re-frame.core :refer [dispatch subscribe]]
            [goog.dom :as dom]
            [parzival.style :refer [ZINDICES]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def main-content-style
  {:grid-area "main-content"
   :position "relative"
   :height "100vh"
   :width "100%"})

(def pdf-container-style
  {:position "absolute"
   :height "100%"
   :overflow "auto"
   :z-index (:zindex-dropdown ZINDICES)
   :left "50%"
   :transform "translateX(-50%)"
   ; ::stylefy/vendors ["webkit"]
   ; ::stylefy/mode [["::-webkit-scrollbar" {:width "20px"}]]
   })

;;; Components

(defn main-content
  []
  (let [pdf? (subscribe [:pdf?])
        url "https://arxiv.org/pdf/2006.06676v2.pdf"]
    (fn []
      (dispatch [:pdf/load url])
      (when @pdf?
        (dispatch [:pdf/view]))
       [:div#main-content (use-style main-content-style)
        [:div#viewerContainer (use-style pdf-container-style)
         [:div#viewer.pdfViewer {:on-mouse-up (fn [] (dispatch [:highlight]))}]]])))

