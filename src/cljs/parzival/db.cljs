(ns parzival.db
  (:require
   [cljs.spec.alpha :as s]
   [clojure.string :as str]
   [parzival.style :refer [HIGHLIGHT-COLOR]]
   ))

(defonce default-db
  {;;; Router
   :current-route :home

   ;;; db/filesystem
   :db/synced? true
   :db/sync-time 15
   :db/filepath nil

   ;;; UI
   :theme/dark? true
   :modal/content nil
   :left-sidebar/open? true
   :right-sidebar/open? false
   :right-sidebar/width 32
   :pagemark/sidebar-open? false
   :pdf/menu-anchor nil ;  {:left :top :height :edit :page}
   :highlight/toolbar-anchor nil ; [x y]
   :highlight/selected nil ; [color page id] [color page id]
   :page/active nil

   ;;; PDF   
   :pdf/viewer nil
   :pdf/worker nil

   ;;; Data
   :pages {}})

;;; Spec

;; Filesystem
;; (s/def :db/synced? boolean?)
;; (s/def :db/sync-time pos-int?)
;; (s/def :db/filepath string?) ;; FIXME actually chech that it's a valid filepath

;;; UI
;; Highlights
(s/def :highlight/anchor (or nil? (s/coll-of nat-int? :kind vector? :count 3)))
;; (s/def :highlight/selected (s/or nil? (s/tuple ))) ;TODO
;; (s/def :theme/dark? boolean?) ;; FIXME change in db to 
;; (s/def :modal/content (or nil? keyword?))
;; (s/def :left-sidebar/open? boolean?)
;; (s/def :right-sidebar/open? boolean?)
;; (s/def :right-sidebar/width pos-int?)
;; (s/def ::pagemark? boolean?)

(s/def ::page-num pos-int?)

;; Pagemarks
(s/def ::deadline string?) ; FIXME
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
(s/def ::page-uid string?) ; FIXME prefix = page-
(s/def ::title string?)
(s/def ::authors string?)
(s/def ::num-pages pos-int?)
(s/def ::filename string?) 
(s/def ::refs (s/coll-of ::block-uid :kind vector?))
(s/def ::modified pos-int?)
(s/def ::added pos-int?)
(s/def :page/active (s/or ::page-uid nil?)) ; FIXME prefix = page-
(s/def ::page (s/keys :req-un [::title ::modified ::added ::refs] 
                      :opt-un [::filename ::authors ::num-pages ::highlights ::pagemarks]))
(s/def ::pages (s/map-of ::page-uid ::page))

;; App-db
(s/def ::db (s/keys :req-un [::pages :page/active]))

;; ;; Pages
;; (s/def ::page-no (s/and integer?
                        ;; #(< 1 %)))
;; (s/def ::page (s/cat :highlights (s/* ::highlight-uid)
                    ;;  :pagemark (s/? ::pagemark)))
;; (s/def ::pages (s/map-of ::page-no ::page))

;; ;; PDF Meta-Data
;; (s/def ::file-name string?) ;FIXME
;; (s/def ::title string?)
;; (s/def ::authors (s/* string?))
 ;; (s/def ::active-page pos-i
;; (s/def ::meta (s/keys :req [::file-name ::title ::authors ::no-pages ::active-page]))

;; ;; PDF
;; (s/def ::pdf (s/keys :req [::meta ::pages ::pagemarks ::highlights]))

;; ;; Blocks
;; (s/def ::block-uid string?) ;FIXME
;; (s/def ::open? boolean?)
;; (s/def ::string string?)
;; (s/def ::highlight ::highlight-uid)
;; (s/def ::parent (s/? ::block-uid))
;; (s/def ::children (s/* ::block-uid))
;; (s/def ::ref string?)
;; (s/def ::refs (s/* ::ref))
;; (s/def ::block (s/keys :req [::block-uid ::open? ::string ::highlight ::parent ::children]))
;; (s/def ::blocks (s/map-of ::block-uid ::block))

;; ;; Documents
;; (s/def ::document-uid string?)
;; (s/def ::root-blocks (s/+ ::block-uid))
;; (s/def ::document (s/keys :req [::title ::root-blocks ::blocks ::pdf]))
;; (s/def ::documents (s/map-of ::document-uid ::document))

;; ;; Refs
;; (s/def ::ref-count pos-int?)
;; (s/def ::link-name string?)
;; (s/def ::link [:documents ::document-uid ::authors idx])
;; (s/def ::link [:documents ::document-uid :blocks ::block-uid])
;; (s/def ::link (s/keys :req  [::document-uid ::block-uid ::ref-count]))
;; (s/def ::links (s/map-of ::link-name ::link))
