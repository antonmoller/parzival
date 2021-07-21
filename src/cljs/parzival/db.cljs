(ns parzival.db)

(defonce default-db
  {:name "re-frame"
   :current-route :home
   :left-sidebar/open true
   :right-sidebar/open false
   :right-sidebar/width 32
   :settings/open false
   :search/anchor nil
   :highlight/anchor nil ; [x y]
   :highlight/selected nil ; [color page id] [color page id]
   :pagemark/anchor nil ;  {:left :top :height :edit :page}
   :pagemark/sidebar '() ; {:start-page :end-page :deadline nil/date}
   :theme/dark true
   :pdf nil
   :pdf/viewer nil
   :pdf/pages 37
   :pdf/width "1000px"
   :pdf/loading? false
   :pdf/highlights {} ; {:color :opacity :x0 :y0 :x1 :y1}
   :pdf/pagemarks {} ; {:page-id {:done nil/{:width :height} :skip :schedule}}
   :pagemark? false
   :loading/progress 0})


; {page-id {highlight-id {:color "green" :start-idx :start-offset :end-idx :end-offset}}}


; {:page-id {:color :width :height :skip? :schedule?}}
; Only one of skip? and schedule? Can be true at one time
; If reader sets pagemark on skip page, remove skip
;; ; Render skip/schedule as line on the right side. NOOOO

;; pagemark ---> {:page-id {:type :skip? :schedule? :width :height :skip? :schedule?}}