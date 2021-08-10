(ns parzival.electron
  (:require
  ;;  [parzival.db :as db]
  ;;  ["electron" :refer [dialog]]
  ;;  ["electron" :refer [remote]]
  ;;  ["electron"]
   [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]))

;; (def electron (js/require "electron"))
;; (def dialog (.. electron -remote -dialog))

;; (reg-event-fx
;;  :fs/open
;;  (fn [_ _]
;;    (js/console.log "OPEN FILESYSTEM")
;;    (js/console.log dialog)
;;   ;;  (js/console.log dialog)
;;    (let [res (.showOpenDialogSync dialog (clj->js {:properties ["openFile"]
;;                                                    :filters [{:name "Pdf" :extensions ["pdf"]}]}))]
;;      (js/console.log res))
;;    ))