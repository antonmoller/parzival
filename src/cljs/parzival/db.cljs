(ns parzival.db)

(defonce default-db
  {:name "re-frame"
   :current-route :home
   :left-sidebar/open true
   :right-sidebar/open false
   :right-sidebar/width 32
   :settings/open false
   :search/open false
   :highlight/open false
   :highlight/anchor nil
   :theme/dark true
   :pdf nil
   :pdf/highlights {}
   :pdf/selection nil
   :loading/progress 0})


; {page-id {highlight-id {:color "green" :start-idx :start-offset :end-idx :end-offset}}}
