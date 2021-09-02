(ns parzival.electron
  (:require
   [parzival.db :as db]
   ["path" :as path]
   ["pdfjs-dist" :as pdfjs]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!]]
   [cognitect.transit :as t]
   [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx reg-sub]]))

(def ipcRenderer (.-ipcRenderer (js/require "electron")))
(def fs (js/require "fs"))
(def DB-INDEX "index.transit")
(def PDFS-DIR-NAME "pdfs")

;;; Filesystem

(defn pdf-dir
  [db-filepath]
  (as-> db-filepath p
    (.dirname path p)
    (.resolve path p PDFS-DIR-NAME)))

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
   (->> (dissoc db :current-route :pdf/active :pdf/viewer :pdf/worker :modal/content :pagemark?)
        (t/write (t/writer :json))
        (.writeFileSync fs db-filepath))))

(reg-fx
 :pdf/create
 (fn [{:keys [filename filepath data worker]}]
   (go
     (try
       (let [pdf (<p! (.-promise (.getDocument pdfjs (js-obj "data" data "worker" worker))))
             meta (<p! (.getMetadata pdf))
             title (.. meta -info -Title)
             author (.. meta -info -Author)
             num-pages (.-numPages pdf)]
         (dispatch [:page/create
                    {:title (if (and (some? title) (not= "" title)) title filename)
                     :num-pages num-pages
                     :authors (if (some? author) author "")
                     :filename filename}])
         (dispatch [:fs/write! filepath data])
         (<p! (.destroy (.-loadingTask pdf))))
       (catch js/Error e (js/console.log (ex-cause e)))))))

(reg-event-fx
 :fs/pdf-add
 (fn [{:keys [db]} _]
   (when-let [pdf-files (.sendSync ipcRenderer "open-pdf-dialog")]
     (let [sync-time (get db :db/sync-time)
           pdf-dir (as-> (get db :db/filepath) p
                     (.dirname path p)
                     (.resolve path p PDFS-DIR-NAME))
           pdfs (.readdirSync fs pdf-dir)
           worker (get db :pdf/worker)]
       {:fx (-> (reduce (fn [m v]
                          (let [pdf-filename (.basename path v)
                                pdf-filepath (.resolve path pdf-dir pdf-filename)]
                            (if (.includes pdfs pdf-filename) ; Don't allow adding duplicate files
                              m
                              (conj m [:pdf/create {:filename pdf-filename :filepath pdf-filepath
                                                    :data (.readFileSync fs v) :worker worker}]))))
                        []
                        pdf-files)
                (conj [:dispatch [:db/not-synced]] [:dispatch-debounce [:fs/pdf-add [:db/sync] sync-time]]))}))))

(reg-event-db
 :db/update-filepath
 (fn [db filepath]
 (js/console.log filepath)  
   (assoc db :db/filepath filepath)))

(reg-event-fx
 :fs/load-db
;;  [check-db]
 (fn [_ [_ db-filepath]]
   (let [db-file (.readFileSync fs db-filepath)
         bkp-filename (str (.getTime (js/Date.)) "-index.transit.bkp")
         bkp-filepath (.resolve path (.dirname path db-filepath) bkp-filename)]

     {:db (-> (t/read (t/reader :json) db-file)
              (assoc :current-route :home :modal/content nil :pdf/active nil :pagemark? false))
      :fx [[:fs/copy! [db-filepath bkp-filepath]]]})))

(reg-event-fx
 :fs/create-new-db
;;  [check-db]
 (fn [_ [_ db-filepath]]
   (as-> (assoc db/default-db :db/filepath db-filepath) db
     {:db db
      :fx [[:fs/write-db! [db db-filepath]]]})))

(reg-event-db
 :db/not-synced
 (fn [db _]
   (assoc db :db/synced? false)))

(reg-event-db
 :db/synced
 (fn [db _]
   (assoc db :db/synced? true)))

(reg-event-fx
 :db/sync
 (fn [{:keys [db]} _]
   (let [db-filepath (get db :db/filepath)]
     {:fx [[:fs/write-db! [db db-filepath]]
           [:dispatch [:db/synced]]]})))

(reg-sub
 :db/synced?
 (fn [db _]
   (:db/synced? db)))

(reg-sub
 :db/sync-time
 (fn [db _]
   (:db/sync-time db)))

(reg-fx
 :electron/quit!
 (fn [_]
   (.send ipcRenderer "exit-app")))

(reg-event-fx
 :boot/desktop
 (fn []
   (let [db-dir (as-> (.sendSync ipcRenderer "document-filepath") documents
                  (.resolve path documents "parzival-db"))
         db-filepath (.resolve path db-dir DB-INDEX)
         db-pdfs (.resolve path db-dir PDFS-DIR-NAME)]
     {:fx [[:fs/create-dir-if-needed! db-dir]
           [:fs/create-dir-if-needed! db-pdfs]
           (if (.existsSync fs db-filepath)
             [:dispatch [:fs/load-db db-filepath]]
             [:dispatch [:fs/create-new-db db-filepath]])]})))

(reg-event-fx
 :quit/desktop
 (fn [{:keys [db]} _]
   (let [db-filepath (get db :db/filepath)]
     {:fx [[:stop-all-debounce]
           [:fs/write-db! [(assoc db :db/synced? true) db-filepath]]
           [:electron/quit!]]})))