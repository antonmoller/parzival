(ns parzival.main.core
  (:require
  ;;  [cljs.core.async :refer [go]]
  ;;  [cljs.core.async.interop :refer [<p!]]
   ["electron" :refer [app BrowserWindow shell ipcMain dialog]]))

(def main-window (atom nil))

(defn init-browser
  []
  (reset! main-window (BrowserWindow.
                       (clj->js {:width 800
                                 :height 600
                                 :minWidth 800
                                 :minHeight 300
                                 :backgroundColor "#1A1A1A"
                                 :resize true
                                  ;; :autoHideMenuBar true
                                  ;; :frame false
                                  ;; :titleBarStyle "hidden"
                                  ;; :trafficLightPosition {:x 19, :y 36}
                                 :webPreferences {:contextIsolation false
                                                  :nodeIntegration true
                                                  :worldSafeExecuteJavaScript true
                                                  :enableRemoteModule true
                                                  :nodeIntegrationWorker true}})))
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js @main-window "closed" #(reset! main-window nil))
  ;; (.openDevTools (.-webContents @main-window)) TODO
  (.. ^js @main-window -webContents (on "new-window" (fn [e url]
                                                       (.. e preventDefault)
                                                       (.. shell (openExternal url))))))


(defn main
  []
  (doto ipcMain
    (.on "exit-app" #(.exit app))
    (.on "document-filepath" #(->> (.getPath app "documents") (set! (.-returnValue %))))
    (.on "open-pdf-dialog" #(->> (.showOpenDialogSync dialog (clj->js {:properties ["openFile" "multiSelections"]
                                                                       :filters [{:name "Pdf" :extensions ["pdf"]}]}))
                                 (set! (.-returnValue %)))))
  ;; (.on app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit app))) TODO
  (.on app "activate" #(when (nil? @main-window) (init-browser)))
  (.on  app "ready" #(init-browser)))
