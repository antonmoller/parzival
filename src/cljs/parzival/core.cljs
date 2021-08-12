(ns parzival.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [parzival.style :as style]
   [parzival.events]
   [parzival.pdf]
   [parzival.electron]
   [parzival.views :as views]
   [parzival.config :as config]
   [parzival.router :as router]
   [stylefy.core :as stylefy]))

(defn dev-setup 
  []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root
  []
  (rf/clear-subscription-cache!)
  (router/init-routes!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init
  []
  (style/init)
  (stylefy/tag "body" style/app-styles)
  (rf/dispatch-sync [:boot/desktop])
  (dev-setup)
  (mount-root)
  (rf/dispatch [:pdf/init-viewer]))


