(ns parzival.main.core
  (:require
            ["electron" :refer [app BrowserWindow shell]]))

(def main-window (atom nil))

(defn init-browser
  []
  (reset! main-window (BrowserWindow.
                        (clj->js {:width 800
                                  :height 600
                                  :autoHideMenuBar true
                                  :webPreferences {:nodeIntegration true
                                                   :worldSafeExecuteJavaScript true
                                                   :nodeIntegrationWorker true}})))
  (.loadURL ^js/electron.BrowserWindow @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js/electron.BrowserWindow @main-window "closed" #(reset! main-window nil))
  (.. ^js @main-window -webContents (on "new-window" (fn [e url]
                                                       (.. e preventDefault)
                                                       (.. shell (openExternal url))))))


(defn main
  []

  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on  app "ready" init-browser))
