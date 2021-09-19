(ns parzival.db
  (:require
   [cljs.spec.alpha :as s]
   [clojure.string :as str]
   [parzival.style :refer [HIGHLIGHT-COLOR]]))

(defonce default-db
  {:current-route :home
   :db/synced? true 
   :db/sync-time 2
   :db/filepath nil
   :theme/dark? true                    
   :modal/content nil                   
   :left-sidebar/open? true            
   :pagemark/sidebar-open? false    
   :right-sidebar/open? false       
   :right-sidebar/width 32           
   :pdf/menu-anchor nil 
   :highlight/toolbar-anchor nil 
   :highlight/selected nil 
   :page/active nil       
   :pdf/viewer nil 
   :pdf/worker nil 
   :pages {}})

;;; Spec

;; Misc
(s/def :theme/dark? boolean?)
(s/def :modal/content (s/nilable keyword?))
(s/def :left-sidebar/open? boolean?)
(s/def :right-sidebar/open? boolean?)
(s/def :right-sidebar/width pos-int?)
(s/def :pagemark/sidebar-open? boolean?)
(s/def :modal/content (s/nilable keyword?))
(s/def :pdf/viewer (s/nilable #(= "PDFViewer" (.. % -constructor -name)))) 
(s/def :pdf/worker (s/nilable #(= "PDFWorker" (.. % -constructor -name)))) 

;; (s/def :pdf/menu-anchor nil?) ;TODO  s/nilable (s/map-of ::edit? (boolean?) ::height (pos-int?) ::left (pos-int?) ::top (pos-int?) ::page-num (nat-int?))
;; (s/def :highlight/toolbar-anchor nil?) ;TODO s/nilable (s/map-of ::left (pos-float?) ::page-right (pos-float?) ::top (pos-float?))
;; (s/def :highlight/selected nil?) ;TODO s/nilable (s/map-of :color (string?) :element)
;; (s/def ::current-route keyword?) ; TODO keyword? or retit.core/match

(s/def ::page-num pos-int?)

;; Filesystem
(s/def :db/synced? boolean?)
(s/def :db/sync-time pos-int?)
(s/def :db/filepath string?) ;; FIXME actually check that it's a valid filepath

;; Pagemarks
(s/def ::deadline string?) ; FIXME (valid date)
(s/def ::skip? true?)
(s/def ::width (s/and float? #(<= 0 % 1)))
(s/def ::height (s/and float? #(<= 0 % 1)))
(s/def ::pagemark (s/keys :req-un [(or ::deadline ::skip? (and ::width ::height))]))
(s/def ::pagemarks (s/map-of ::page-num ::pagemark))

;; Highlights
(s/def ::highlight-uid (s/and string? #(str/starts-with? % "highlight-")))
(s/def ::color #(contains? HIGHLIGHT-COLOR %))
(s/def ::start nat-int?)
(s/def ::end nat-int?)
(s/def ::start-offset nat-int?)
(s/def ::end-offset nat-int?)
(s/def ::highlight (s/keys :req-un [::color ::start ::start-offset ::end ::end-offset]))
(s/def ::highlights (s/map-of ::page-num (s/map-of ::highlight-uid ::highlight)))

;; Blocks
(s/def ::block-uid string?)

;; Pages
(s/def ::page-uid (s/and string? #(str/starts-with? % "page-"))) 
(s/def ::title string?)
(s/def ::authors string?)
(s/def ::num-pages pos-int?)
(s/def ::filename string?) 
(s/def ::refs (s/coll-of ::block-uid :kind vector?))
(s/def ::modified pos-int?)
(s/def ::added pos-int?)
(s/def :page/active (s/nilable ::page-uid)) 
(s/def ::page (s/keys :req-un [::title ::modified ::added ::refs] 
                      :opt-un [::filename ::authors ::num-pages ::highlights ::pagemarks]))
(s/def ::pages (s/map-of ::page-uid ::page))

;; App-db
(s/def ::db (s/keys :req [:page/active :db/synced? :db/sync-time :db/filepath
                          :right-sidebar/open? :right-sidebar/width :left-sidebar/open?
                          :theme/dark?  :pagemark/sidebar-open? :pdf/menu-anchor
                          :highlight/toolbar-anchor :highlight/selected :pdf/viewer :pdf/worker]
                    :req-un [::current-route ::pages]))