(ns parzival.db)

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
   :page/active nil
   :refs {}
   :pages {}})

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