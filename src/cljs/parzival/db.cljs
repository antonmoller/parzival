(ns parzival.db
  (:require
   [cljs.spec.alpha :as s]))

(defonce default-db
  {:name "re-frame"
   :current-route :home
   :path-to-dir ""
   :theme/dark true
   :left-sidebar/open true
   :right-sidebar/open false
   :right-sidebar/width 32
   :settings/open false
   :highlight/anchor nil ; [x y]
   :highlight/selected nil ; [color page id] [color page id]
   :pagemark? false
   :pagemark/anchor nil ;  {:left :top :height :edit :page}
   :pdf nil
   :pdf/document nil
   :pdf/viewer nil
   :pdf/loading? false
   :pdf/highlights {}
  ;;  :page/active nil
  ;;  :refs {}
  ;;  :pages {}
   
   })

;;; Spec

;; ;; UI
;; (s/def :theme/dark boolean?)
;; (s/def :left-sidebar/open? boolean?)
;; (s/def :right-sidebar/open? boolean?)
;; (s/def :right-sidebar/width pos-int?)
;; (s/def :settings/open? boolean?)
;; (s/def :highlight/anchor (s/cat :x (and pos? float?) :y (and pos? float?)))
;; (s/def :highlight/selected ())
;; ;; (s/def ::highlight/selected)
;; ;; (s/def ::pagemark?)
;; ;; (s/def :pdf/active)
;; ;; (s/def :pdf/document)
;; ;; (s/def :pdf/viewer)
;; ;; (s/def :document/active)


;; ;; Pagemarks
;; (s/def ::pagemark-uid string?) ; FIXME
;; (s/def ::deadline string?) ; FIXME
;; (s/def ::skip? true?)
;; (s/def ::width (s/and float? #(<= 0 % 1)))
;; (s/def ::height (s/and float? #(<= 0 % 1)))
;; (s/def ::pagemark (s/keys :req [(or ::deadline ::skip? (and ::width ::height))]))
;; (s/def ::pagemarks (s/map-of ::pagemark-uid ::pagemark))

;; Highlights
(s/def ::highlight-uid string?)
(s/def ::color #{:orange :green :blue :purple})
(s/def ::opacity #(<= 0.1 % 1))
(s/def ::start pos-int?)
(s/def ::end pos-int?)
(s/def ::start-offset pos-int?)
(s/def ::end-offset pos-int?)
(s/def ::highlight (s/keys :req [::color ::opacity ::start ::end ::start-offset ::end-offset]))
(s/def ::highlights (s/map-of ::highlight-uid ::highlight))

(s/def ::db (s/keys :req []))

;; ;; Pages
;; (s/def ::page-no pos-int?)
;; (s/def ::page (s/cat :highlights (s/* ::highlight-uid)
;;                      :pagemark (s/? ::pagemark)))
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

;; ;; App-db
;; (s/def ::db (s/keys :req [::links ::documents]))




;; {:refs {ref-name [[:path :to :ref]]};authors ;if empty vector remove editing author edits meta of pdfs
;;  :pages {pages-id {:title ""
;;                    :root-blocks [] ; if pdf nil and root-blocks empty remove
;;                    :blocks {block-uid {:open? true
;;                                        :string ""
;;                                        :highlight-ref ""
;;                                        :parent block-uid
;;                                        :children [block-uid]
;;                                        :refs #{tags}}}
;;                    :pdf {:meta {:title "" :authors [] :no-pages 37 :active-page 1 :file-name ""}
;;                          :pages {page-no {:pagemark pagemark-uid :highlights [highlight-uid]}}
;;                          :pagemarks {pagemark-uid {:type :deadline :width :height}}
;;                          :highlights {highlight-uid {:type :start-row :start-offset :end-row :end-offset}
;;                                       :pages {p}}}}}

;; (def schema
;;   {;;; Pdf
;;    :pdf/uid {:db/cardinality :db.cardinality/one
;;              :db/unique :db.unique/identity
;;              :db/doc "The identifier for the pdf"}
;;    ;TODO: Filepath "The path to the pdf in the filesystem e.g.
;;    ;        \\Path\\To\\Pdf"}
;;    ; TODO: Use spec to enforce the types of the attributes
;;    :pdf/meta {:db/cardinality :db.cardinality/one
;;               :db/type :db.type/tuple
;;               :db/tupleAttrs [:title :authors :no-pages :active-page :file-name]
;;               :db/doc ":title The title of the pdf (string).
;;                        :authors References the authors of the pdf (ref)).
;;                        :no-pages The number of pages in the pdf (1-indexed) (long).
;;                        :active-page The page that was last active when the pdf was 
;;                        closed [1 no-pages] (long).
;;                        :file-name The filename that's stored in the filesystem (string)"}
;;    :pdf/pages {:db/cardinality :db.cardinality/many
;;                :db/valueType :db.type/ref
;;                :db/isComponent true
;;                :db/doc "A page exists if there are at least one pagemark or one highlight,
;;                         otherwise it should not exist.  Must be in the interval [1 no-pages].
;;                         Each page can have only one pagemark, but it can have multiple highlights.
;;                         Highlights within one page cannot overlap.  Attributes: :page/no 
;;                         :page/pagemark :page/highlights"}
;;    ;;; Pagemark
;;    :pagemark/uid {:db/unique :db.unique/identity
;;                   :db/cardinality :db.cardinality/one
;;                   :db/type :db.type/string
;;                   :db/doc "The identifier for the pagemark.  Prefixed with 'pagemark-'"}
;;    :pagemark/data {:db/cardinality :db.cardinality/one
;;                    :db/type :db.type/tuple
;;                    :db/tupleAttrs [:type :deadline :width :height]
;;                    :db/doc "A tuple containing pagemark attributes"}
;;    ;;; Highlight
;;    :highlight/uid {:db/unique :db.unique/identity
;;                    :db/doc "The identifier for the highlight"}
;;    :highlight/block {:db/cardinality :db.cardinality/one
;;                      :db/type :db.type/ref
;;                      :db/doc "Each highlight will be at the top of a block in the
;;                               right-sidebar"}
;;    :highlight/data {:db/cardinality :db.cardinality/one
;;                     :db/type :db.type/tuple
;;                     :db/tupleAttrs [:type :start-row :start-offset :end-row :end-offset]
;;                     :db/doc "All indexes are 0-indexed
;;                              :type corresponds to the color and makes highlight-templates possible.
;;                              :start-row the row on which the highlight starts.
;;                              :start-offset how many characters that's skipped before the highlight 
;;                                            starts.
;;                              :end-row the row on which the highligh ends.
;;                              :end-offset how many characters to highlight on the last row before 
;;                                          the highlight stops."}

;;                           })