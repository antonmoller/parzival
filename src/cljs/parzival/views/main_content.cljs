(ns parzival.views.main-content
  (:require
            [re-frame.core :refer [dispatch subscribe]]
            [goog.dom :as dom]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def main-content-style
  {:grid-area "main-content"
   :position "static"
   ; :display "flex"
   ; :flex "1 1 100%"
   :margin-top "2.5rem"
   :margin-left "4rem"
   })

(def pdf-container-style
  {:overflow "auto"
   :height "100vh"
   ; :width "100vw"
   ; :display "flex"
   ; :flex-direction "column"
   :position "absolute"
   ; :margin-top "2.5rem"
   ; :margin-left "10rem"
   ; :top "50%"
   ; :left "50%"
   ; :transform "translate(-50%, -50%)"
   ; :transform "translateX(-50%)"
   ; :margin-left "auto"
   ; :margin-right "auto"
   ; ::stylefy/vendors ["webkit"]
   ; ::stylefy/mode [["::-webkit-scrollbar" {:width "20px"}]]
   })

(def canvas-style
  {:box-sizing "border-box"
   :padding-bottom "10px"})

;;; Utils

(defn highlighter
  [highlights]
  (dispatch [:render/highlight highlights]))

;;; Components

(defn main-content
  []
  (let [pdf? (subscribe [:pdf?])
        ;prog (subscribe [:loading/progress])
        ; url "https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/examples/learning/helloworld.pdf"
        ; pdf-container (dom/getDocument)
        url "https://arxiv.org/pdf/2006.06676v2.pdf"
        ]
    (fn []
      (dispatch [:pdf/load url])
      (when @pdf?
        (dispatch [:pdf/view]))
       [:div#main-content (use-style main-content-style)
        [:div#viewerContainer (use-style pdf-container-style)
         [:div#viewer.pdfViewer {:on-mouse-up (fn [] (dispatch [:highlight]))}
          ]]])))

