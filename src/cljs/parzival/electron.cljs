(ns parzival.electron
  (:require
   [parzival.db :as db]
   [parzival.pdf :refer [check-spec-interceptor]]
  ;;  ["electron" :refer [dialog]]
  ;;  ["electron" :refer [remote]]
  ;;  ["electron"]
   [cognitect.transit :as t]
   ["path" :as path]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]))

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

(defn gen-uid
  [prefix]
  (str prefix "-" (random-uuid)))

(reg-event-db
 :document/create
 [check-spec-interceptor]
 (fn [db [_ title filename]]
   (assoc-in db [:documents (gen-uid "document")] {:title title :filename filename})))

(defn pdf-dir
  [db-filepath]
  (as-> db-filepath p
    (.dirname path p)
    (.resolve path p PDFS-DIR-NAME)))

(reg-event-db
 :pdf/active
 (fn [db [_ filename]]
   (assoc db :pdf/active filename)))

(reg-event-fx
 :fs/pdf-add
 (fn [{:keys [db]} _]
   (let [pdf-dir (as-> (get db :db/filepath) p
                   (.dirname path p)
                   (.resolve path p PDFS-DIR-NAME))
         res (.showOpenDialogSync dialog (clj->js {:properties ["openFile"]
                                                   :filters [{:name "Pdf" :extensions ["pdf"]}]}))
         pdf-file (first res)
         pdf-filename (.basename path pdf-file)
         pdf-filepath (.resolve path pdf-dir pdf-filename)]
     (.copyFileSync fs pdf-file pdf-filepath)
     {:fx [[:dispatch [:document/create pdf-filename pdf-filename]]]})))

(reg-event-fx
 :pdf/full-path
 (fn [{:keys [db]} [_ pdf-filename]]
   (let [pdf-dir (pdf-dir (get db :db/filepath))
         pdf-filepath (.resolve path pdf-dir pdf-filename)]
     (js/console.log pdf-filepath)
     {:db (assoc db :pdf/data (.readFileSync fs pdf-filepath))})))

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
             [:dispatch [:fs/create-new-db db-filepath]])
           ]})))