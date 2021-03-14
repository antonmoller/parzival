(ns parzival.db)

(defonce default-db
  {:name "re-frame"
   :left-sidebar/open true
   :right-sidebar/open false
   :right-sidebar/width 32
   :settings/open false
   :search/open false
   :theme/dark true
   :pdf nil
   :pdf/viewer nil
   :pdf/highlights {}
   :loading/progress 0})


; {page-id {highlight-id {:color "green" :start-pos [left top offset] :end-pos [left top offset]}}}
