(ns parzival.db)

(defonce default-db
  {:name "re-frame"
   :current-route :home
   :left-sidebar/open true
   :right-sidebar/open false
   :right-sidebar/width 32
   :settings/open false
   :search/open false
   :highlight/anchor nil ; [x y]
   :highlight/selected nil ; [color page id] [color page id]
   :theme/dark true
   :pdf nil
   :pdf/highlights {} ; {:color :opacity :x0 :y0 :x1 :y1}
   :loading/progress 0})


; {page-id {highlight-id {:color "green" :start-idx :start-offset :end-idx :end-offset}}}
