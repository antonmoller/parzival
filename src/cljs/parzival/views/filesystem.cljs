(ns parzival.views.filesystem
  (:require
   [parzival.views.modal :refer [modal]]
   [parzival.views.buttons :refer [button]]
   ["@material-ui/icons/Save" :default Save]
   [stylefy.core :as stylefy :refer [use-style]]))

;; (def electron (js/require "electron"))
(def app (-> (js/require "electron")
             (.. -remote -app)))

(def os (js/require "os"))

;; (def remote (.-remote electron))
;; (def app (.-app remote))

;;; Styles

(def filesystem-style
  {:display "flex"
   :flex-direction "column"
   :justify-content "space-between"
   :box-sizing "border-box"
   :padding "2rem"
   :width "27rem"})

(def drop-style
  {:display "flex"
   :flex-direction "column"
   :justify-content "space-between"
   :align-items "center"
   :height "8rem"})

;;; Components

(defn filesystem
  []
  [modal
   {:id "filesystem-modal"
    :open? :fs/open?
    :toggle :fs/toggle
    :content [:div (use-style filesystem-style)
              [:div (use-style drop-style)
               [:h4  "Drag and Drop " [:kbd "PDF Files"] " here to add"]
               [:> Save {:style {:font-size "5em"}}]]
              [button {:on-click (fn [_]
                                   (.click (.getElementById js/document "file-upload"))
                                   (js/console.log "hi")
                                  ;;  (js/console.log electron)
                                   (js/console.log app)
                                  ;;  (js/console.log remote)
                                   (js/console.log (.getPath app "documents"))
                                   (js/console.log (.homedir os))
                                  ;;  (js/console.log electron)
                                  ;;  (js/console.log (.homeDir os))
                                  ;;  (js/console.log (.. js/window -location -pathName))
                                  ;;  (js/console.log (.getPath app "documents"))
                                  ;;  (let [DOC-PATH (.getPath app "documents")]
                                  ;;    (js/console.log DOC-PATH))
                                  ;;  (js/console.log path)
                                  ;;  (js/console.log (.baseName path))
                                  ;;  (js/console.log (.getPath app "documents"))
                                  ;;  (js/console.log (str "file://" js/__dirname "/public/index.html"))
                                    ;; (.join path (js/__dirname, "Documents")))

                                   )
                       :primary "true"
                       :style {:width "7em"}}
               [:span "Browse Files"]]
              [:input {:id "file-upload" :type "file" :style {:display "none"}}]]}])