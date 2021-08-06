(ns parzival.main.core
  (:require
   ["electron" :refer [app BrowserWindow shell]]))

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
  (.. ^js @main-window -webContents (on "new-window" (fn [e url]
                                                       (.. e preventDefault)
                                                       (.. shell (openExternal url))))))


(defn main
  []
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit app)))
  (.on app "activate" #(when (nil? @main-window) (init-browser)))
  (.on  app "ready" #(init-browser)))
