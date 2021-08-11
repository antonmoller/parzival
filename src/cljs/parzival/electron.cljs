(ns parzival.electron
  (:require
   [parzival.db :as db]
  ;;  ["electron" :refer [dialog]]
  ;;  ["electron" :refer [remote]]
  ;;  ["electron"]
   [cognitect.transit :as t]
   ["path" :as path]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx reg-cofx]]))

(def electron (js/require "electron"))
(def remote (.-remote electron))

(def app (.-app remote))
(def dialog (.-dialog remote))

(def fs (js/require "fs"))

(def DB-INDEX "index.transit")
(def PDFS-DIR-NAME "pdfs")

(def documents-parzival-dir
  (let [doc-path (.getPath app "documents")]
    (.resolve path doc-path "parzival-db")))

(reg-event-fx
 :fs/pdf-add
 (fn [_ _]
   (let [res (.showOpenDialogSync dialog (clj->js {:properties ["openFile"]
                                                   :filters [{:name "Pdf" :extensions ["transit"]}]}))]
     (js/console.log documents-parzival-dir)
     (js/console.log res))))

(reg-fx
 :fs/write!
 (fn [[db db-filepath]]
   (->> (t/write (t/writer :json) db)
        (.writeFileSync fs db-filepath))))

(reg-fx
 :fs/create-dir-if-needed!
 (fn [dir]
   (when-not (.existsSync fs dir)
     (.mkdirSync fs dir))))

(reg-event-db
 :db/update-filepath
 (fn [db filepath]
   (assoc db :db/filepath filepath)))

(reg-event-fx
 :fs/read-db
 (fn [coeffects db-path]
   (->> (.readFileSync fs db-path)
        (t/read (t/reader :json))
        (assoc coeffects :fs/db))))

(reg-event-db
 :fs/load-db
 (fn [_ [_ db-filepath]]
   (->> (.readFileSync fs db-filepath)
        (t/read (t/reader :json)))))

(reg-event-fx
 :fs/create-new-db
 (fn [_ [_ db-filepath]]
   {:db (assoc db/default-db :db/filepath db-filepath)
    :fs/write! [db/default-db db-filepath]}))

(reg-event-fx
 :boot/desktop
 (fn []
   (let [db-filepath (.resolve path documents-parzival-dir DB-INDEX)
         db-pdfs (.resolve path documents-parzival-dir PDFS-DIR-NAME)]
     {:fx [[:fs/create-dir-if-needed! documents-parzival-dir]
           [:fs/create-dir-if-needed! db-pdfs]
           (if (.existsSync fs db-filepath)
             [:dispatch [:fs/load-db db-filepath]]
             [:dispatch [:fs/create-new-db db-filepath]])]})))