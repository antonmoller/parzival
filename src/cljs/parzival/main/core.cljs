(ns parzival.main.core
  (:require
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
                                                  ;; :preload (.join path (.getAppPath app) "preload.js")
                                                  :nodeIntegration true}})))
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js @main-window "closed" #(reset! main-window nil))
  (.openDevTools (.-webContents ^js @main-window))
  (.. ^js @main-window -webContents (on "new-window" (fn [e url]
                                                       (.preventDefault e)
                                                       (.openExternal shell url)))))


(defn main
  []
  (doto ipcMain
    (.on "exit-app" #(when-not (= js/process.platform "darwin") (.exit app)))
    (.on "document-filepath" #(->> (.getPath app "documents") (set! (.-returnValue %))))
    (.on "open-pdf-dialog" #(->> (.showOpenDialogSync dialog (clj->js {:properties ["openFile" "multiSelections"]
                                                                       :filters [{:name "Pdf" :extensions ["pdf"]}]}))
                                 (set! (.-returnValue %)))))
  (.on app "activate" #(when (nil? @main-window) (init-browser)))
  (.on  app "ready" #(init-browser)))
