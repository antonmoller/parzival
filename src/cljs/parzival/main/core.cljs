(ns parzival.main.core
  (:require
            ["electron" :refer [app BrowserWindow shell]]))

(def main-window (atom nil))

(defn init-browser
  []
  (reset! main-window (BrowserWindow.
                        (clj->js {:width 800
                                  :height 600
                                  :resize true
                                  :autoHideMenuBar true
                                  :enableRemoteModule true
                                  :webPreferences {:nodeIntegration true
                                                   :worldSafeExecuteJavaScript true
                                                   :enableRemoteModule true
                                                   :nodeIntegrationWorker true}})))
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js @main-window "closed" #(reset! main-window nil))
  (.. ^js @main-window -webContents (on "new-window" (fn [e url]
                                                       (.. e preventDefault)
                                                       (.. shell (openExternal url))))))


(defn main
  []
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "activate" (fn []
                        (when (nil? @main-window)
                          (init-browser))))
  (.on  app "ready" (fn []
                      (init-browser))))
