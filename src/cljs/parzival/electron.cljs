(ns parzival.electron
  (:require
   [parzival.db :as db]
   [parzival.pdf :refer [check-spec-interceptor]]
  ;;  ["electron" :refer [dialog]]
  ;;  ["electron" :refer [remote]]
  ;;  ["electron"]
   [cognitect.transit :as t]
   ["path" :as path]
   ["pdfjs-dist" :as pdfjs]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!]]
   [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]))

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

;; TODO also add authors to links (ref-count)
(reg-event-db
 :document/create
 (fn [db [_ title authors filename]]
   (as-> (.getTime (js/Date.)) timestamp
     (assoc-in db [:documents (gen-uid "document")] {:title title
                                                     :authors authors
                                                     :filename filename
                                                     :modified timestamp
                                                     :added timestamp}))))

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
 :fs/write!
 (fn [_ [_ filepath file]]
   (.writeFileSync fs filepath file)))

(reg-fx
 :fs/create-dir-if-needed!
 (fn [dir]
   (when-not (.existsSync fs dir)
     (.mkdirSync fs dir))))

(reg-fx
 :fs/write-db!
 (fn [[db db-filepath]]
   (->> (dissoc db :pdf/viewer nil :pdf/worker nil)
        (t/write (t/writer :json))
        (.writeFileSync fs db-filepath))))

(reg-fx
 :pdf/create
 (fn [{:keys [filename filepath data worker]}]
   (go
     (try
       (let [pdf (<p! (.-promise (.getDocument pdfjs (js-obj "data" data
                                                             "worker" worker))))
             meta (<p! (.getMetadata pdf))
             title (.. meta -info -Title)
             author (.. meta -info -Author)]
         (dispatch [:document/create
                    (if (and (some? title) (not= "" title)) title filename)
                    (if (and (some? author) (not= "" author)) [author] [])
                    filename])
         (dispatch [:fs/write! filepath data])
         (<p! (.destroy (.-loadingTask pdf))))
       (catch js/Error e (js/console.log (ex-cause e)))))))

(reg-event-fx
 :fs/pdf-add
 [check-spec-interceptor]
 (fn [{:keys [db]} [_ pdf-files]]
   (when pdf-files
     (let [pdf-dir (as-> (get db :db/filepath) p
                     (.dirname path p)
                     (.resolve path p PDFS-DIR-NAME))
           pdfs (.readdirSync fs pdf-dir)
           worker (get db :pdf/worker)]
       {:fx (reduce (fn [m v]
                      (let [pdf-filename (.basename path v)
                            pdf-filepath (.resolve path pdf-dir pdf-filename)]
                        (if (.includes pdfs pdf-filename) ; Don't allow adding duplicate files
                          m
                          (conj m [:pdf/create {:filename pdf-filename :filepath pdf-filepath
                                                :data (.readFileSync fs v) :worker worker}]))))
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
     {:db (assoc db :pdf/data (.readFileSync fs pdf-filepath))})))

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
 [check-spec-interceptor]
 (fn [_ [_ db-filepath]]
   (as-> (assoc db/default-db :db/filepath db-filepath) db
     {:db db
      :fs/write-db! [db db-filepath]})))

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