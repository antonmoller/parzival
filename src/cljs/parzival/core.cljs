(ns parzival.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [parzival.style :as style]
   [parzival.events :as events]
   [parzival.views :as views]
   [parzival.config :as config]
   [stylefy.core :as stylefy]))


(defn dev-setup 
  []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root 
  []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init 
  []
  (style/init)
  (stylefy/tag "body" style/app-styles)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))

