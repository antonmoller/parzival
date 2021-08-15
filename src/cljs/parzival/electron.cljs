(ns parzival.electron
  (:require
   [parzival.db :as db]
   [parzival.pdf :refer [check-spec-interceptor]]
  ;;  ["electron" :refer [dialog]]
  ;;  ["electron" :refer [remote]]
  ;;  ["electron" :refer ]
   [parzival.utils :refer [gen-uid]]
   [cognitect.transit :as t]
   ["path" :as path]
  ;;  ["fs" :as fs]
  ;;  ["electron" :refer [ipcRenderer]]
   ["pdfjs-dist" :as pdfjs]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer [<p!]]
   [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]))

(def ipcRenderer (.-ipcRenderer (js/require "electron")))
(def fs (js/require "fs"))

(def DB-INDEX "index.transit")
(def PDFS-DIR-NAME "pdfs")

(reg-event-db
 :link/create
 (fn [db [_ link-name link]]
   (update-in db [:links link-name] conj link)))

(reg-event-fx
 :document/create
 (fn [{:keys [db]} [_ title authors filename]]
   (let [uid (gen-uid "document")
         timestamp (.getTime (js/Date.))]
     {:db (assoc-in db [:documents uid] {:title title :authors authors :filename filename
                                         :modified timestamp :added timestamp
                                         :highlights {} :pagemarks {}})
      :fx [(when-not (= [] authors)
             [:dispatch [:link/create (first authors) [:documents uid :authors 0]]])]})))

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
   (->> (dissoc db :current-route :pdf/viewer :pdf/worker)
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
;;  [check-spec-interceptor]
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
;;  [check-spec-interceptor]
 (fn [_ [_ db-filepath]]
   (as-> (.readFileSync fs db-filepath) db
     (t/read (t/reader :json) db)
     (assoc db :current-route :home))))

(reg-event-fx
 :fs/create-new-db
 [check-spec-interceptor]
 (fn [_ [_ db-filepath]]
   (as-> (assoc db/default-db :db/filepath db-filepath) db
     {:db db
      :fx [[:fs/write-db! [db db-filepath]]
           [:dispatch [:db/synced]]]})))

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

(reg-fx
 :electron/quit!
 (fn [_]
   (.send ipcRenderer "exit-app")))

(defonce timeouts
  (atom {}))

(reg-fx
 :dispatch-debounce
 (fn [[id event-vec s]]
   (js/clearTimeout (@timeouts id))
   (swap! timeouts assoc id
          (js/setTimeout (fn []
                           (dispatch event-vec)
                           (swap! timeouts dissoc id))
                         (* 1000 s)))))

(reg-fx
 :stop-all-debounce
 (fn [_]
   (run! (fn [[_ v]] (js/clearTimeout v)) @timeouts)
   (reset! timeouts nil)))

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
        ;;  [:dispatch [:modal/close-all]]
           [:fs/write-db! [(assoc db :db/synced? true) db-filepath]]
           [:electron/quit!]]})))