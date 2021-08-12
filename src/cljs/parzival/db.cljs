(ns parzival.db
  (:require
   [cljs.spec.alpha :as s]))

(defonce default-db
  {:name "parzival"
   :current-route :home
   :db/filepath nil
   :theme/dark true
   :left-sidebar/open true
   :right-sidebar/open false
   :right-sidebar/width 32
   :search/open? false
   :fs/open? false
   :settings/open false
   :pagemark? false
   :pagemark/anchor nil ;  {:left :top :height :edit :page}
   :pdf nil
   :pdf/document nil
   :pdf/viewer nil
   :pdf/loading? false
   :highlight/anchor nil ; [x y]
   :highlight/selected nil ; [color page id] [color page id]
  ;;  :page/active nil
  ;;  :refs {}
   :pdf/data nil
   :pdf/active nil
   
   :documents {}
   :pagemarks (sorted-map)
   :highlights (sorted-map)})

;;; Spec

;; ;; UI
;; (s/def ::name string?)
;; (s/def ::current-route keyword?)
;; (s/def :theme/dark boolean?)
;; (s/def :left-sidebar/open boolean?)
;; (s/def :right-sidebar/open boolean?)
;; (s/def :right-sidebar/width pos-int?)
;; (s/def :settings/open boolean?)
;; (s/def ::pagemark? boolean?)
;; (s/def :pagemark/anchor (s/or nil?
;;                               (s/keys :req-un [::left ::top ::height ::edit ::page])))
;; (s/def :)
;; (s/def )
;; (s/def :highlight/anchor (s/cat :x (and pos? float?) :y (and pos? float?)))
;; (s/def :highlight/selected ())
;; ;; (s/def ::highlight/selected)
;; ;; (s/def ::pagemark?)
;; ;; (s/def :pdf/active)
;; ;; (s/def :pdf/document)
;; ;; (s/def :pdf/viewer)
;; ;; (s/def :document/active)

(s/def ::page-no pos-int?)

;; Pagemarks
(s/def ::pagemark-uid string?) ; FIXME prefix = pagemark-
(s/def ::deadline string?) ; FIXME
(s/def ::skip? true?)
(s/def ::width (s/and float? #(<= 0 % 1)))
(s/def ::height (s/and float? #(<= 0 % 1)))
(s/def ::pagemark (s/keys :req-un [(or ::deadline ::skip? (and ::width ::height))]))
(s/def ::pagemarks (s/and (s/map-of ::page-no (s/map-of ::pagemark-uid ::pagemark))
                          #(instance? PersistentTreeMap %)))

;; Highlights
(s/def ::highlight-uid string?) ; FIXME prefix = highlight-
(s/def ::color #{:orange :green :blue :purple})
(s/def ::start nat-int?)
(s/def ::end nat-int?)
(s/def ::start-offset nat-int?)
(s/def ::end-offset nat-int?)
(s/def ::highlight (s/keys :req-un [::color ::start ::start-offset ::end ::end-offset]))
(s/def ::highlights (s/and (s/map-of ::page-no (s/map-of ::highlight-uid ::highlight))
                           #(instance? PersistentTreeMap %)))

;; Documents
(s/def ::document-uid string?) ; FIXME prefix = document-
(s/def ::title string?)
(s/def ::filename string?) ;FIXME Can also be nil if documents has been removed should check that it's a path
(s/def ::document (s/keys :req-un [::title ::filename]))
(s/def ::documents (s/map-of ::document-uid ::document))

;; App-db
(s/def ::db (s/keys :req-un [::highlights ::pagemarks ::documents]))

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
;; (s/def ::no-pages pos-int?)
;; (s/def ::active-page pos-int?)
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
;; (s/def ::link (s/keys :req  [::document-uid ::block-uid ::ref-count]))
;; (s/def ::links (s/map-of ::link-name ::link))
