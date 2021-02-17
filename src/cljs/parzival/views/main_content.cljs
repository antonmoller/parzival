(ns parzival.views.main-content
  (:require
            [re-frame.core :refer [dispatch subscribe]]
            [goog.dom :as dom]
            [stylefy.core :as stylefy :refer [use-style]]))

;;; Styles

(def main-content-style
  {:grid-area "main-content"
   :display "flex"
   :flex "1 1 100%"
   ; :padding-left "100px"
   ; :align-items "stretch"
   ; :justify-content "stretch"
   ; :margin "40px"
   ; :overflow-y "auto"
   ; :padding-top "2.5rem" ; TODO: Change the padding stuff
   ; :padding-left "2.5rem"
   ; :padding-right "2.5rem"
   ; :margin-left "40px"
   ; :margin "40px"
   })

(def pdf-container-style
  {:overflow-y "auto"
   :height "100vh"
   :display "flex"
   :flex-direction "column"
   :box-sizing "border-box" ; TODO: Kind of ugly since it will make the pdf slightly off-center
   :padding-left "69px"
   :margin "0 auto"
   ; ::stylefy/vendors ["webkit"]
   ; ::stylefy/mode [["::-webkit-scrollbar" {:width "20px"}]]
   })

(def canvas-style
  {:box-sizing "border-box"
   :padding-bottom "10px"})

;;; Helpers

;;; Components

; [:div#pdf-container (use-style pdf-container-style)
;  [:div
;   [:canvas]]

; (defn pdf-container
;   {[:div#pdf-container (use-style pdf-container-style)
;     [:canvas#canvas-0 (use-style canvas-style)]
;     [:canvas#canvas-1 (use-style canvas-style)]
;     [:canvas#canvas-2 (use-style canvas-style)]
;     [:canvas#canvas-3 (use-style canvas-style)]
;     [:canvas#canvas-4 (use-style canvas-style)]]})

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
        (dispatch [:pdf/render 10]))
       [:div (use-style main-content-style)
        [:div#canvas-container (use-style pdf-container-style)]])))


;TODO: REnder first pages correctly
