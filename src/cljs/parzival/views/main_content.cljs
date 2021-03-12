(ns parzival.views.main-content
  (:require
            [re-frame.core :refer [dispatch subscribe]]
            [goog.dom :as dom]
            [parzival.style :refer [ZINDICES]]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def main-content-style
  {:grid-area "main-content"
   :display "flex"
   :justify-content "center"})

(def pdf-container-style
  {:position "absolute"
   :height "100%"
   :overflow-y "auto"})

;;; Components

(defn main-content
  []
  (let [pdf? (subscribe [:pdf?])
        url "https://arxiv.org/pdf/2006.06676v2.pdf"]
    (fn []
      (dispatch [:pdf/load url])
      (when @pdf?
        (dispatch [:pdf/view]))
       [:div (use-style main-content-style)
        [:div#viewerContainer (use-style pdf-container-style)
         [:div#viewer.pdfViewer {:on-mouse-up (fn [] (dispatch [:highlight]))}]]])))

