(ns parzival.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [parzival.style :as style]
   [parzival.utils :as utils]
   [parzival.events]
   [parzival.listeners]
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
  (if (utils/electron?)
    (do
      (rf/dispatch-sync [:listeners/init])
      (rf/dispatch-sync [:boot/desktop]))
    (rf/dispatch-sync [:boot/web]))
  (dev-setup)
  (mount-root)
  (rf/dispatch [:pdf/init-viewer]))


