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

(reg-fx
 :fs/copy!
 (fn [[file-path copy-file-path]]
   (.copyFileSync fs file-path copy-file-path)))

(reg-event-fx
 :fs/pdf-add
 (fn [{:keys [db]} [_ pdf-files]]
   (when pdf-files
     (let [pdf-dir (as-> (get db :db/filepath) p
                     (.dirname path p)
                     (.resolve path p PDFS-DIR-NAME))
           pdfs (.readdirSync fs pdf-dir)]
       {:fx (reduce (fn [m v]
                      (let [pdf-filename (.basename path v)
                            pdf-filepath (.resolve path pdf-dir pdf-filename)]
                        (if (.includes pdfs pdf-filename) ; Don't allow adding duplicate files
                          m
                          (conj m
                                [:fs/copy! [v pdf-filepath]]
                                [:dispatch [:document/create pdf-filename pdf-filename]]))))
                    []
                    pdf-files)}))))

(reg-event-fx
 :fs/pdf-dialog
 (fn []
   (let [pdf-files (.showOpenDialogSync dialog (clj->js {:properties ["openFile" "multiSelections"]
                                                         :filters [{:name "Pdf" :extensions ["pdf"]}]}))]
     {:dispatch [:fs/pdf-add pdf-files]})))

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
   (->> (dissoc db :pdf/viewer nil :pdf/worker nil)
        (t/write (t/writer :json))
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
   (as-> (assoc db/default-db :db/filepath db-filepath) db
     {:db db
      :fs/write! [db db-filepath]})))

(reg-event-fx
 :boot/desktop
;;  [check-spec-interceptor]
 (fn []
   (let [db-filepath (.resolve path documents-parzival-dir DB-INDEX)
         db-pdfs (.resolve path documents-parzival-dir PDFS-DIR-NAME)]
     {:fx [[:fs/create-dir-if-needed! documents-parzival-dir]
           [:fs/create-dir-if-needed! db-pdfs]
           (if (.existsSync fs db-filepath)
             [:dispatch [:fs/load-db db-filepath]]
             [:dispatch [:fs/create-new-db db-filepath]])]})))