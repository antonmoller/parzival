(ns parzival.db
  (:require
   [datascript.core :as d]
   [reagent.core :as r]))

(defonce default-db
  {:name "re-frame"
   :current-route :home
   :left-sidebar/open true
   :right-sidebar/open false
   :right-sidebar/width 32 ;---
   :settings/open false
   :search/anchor nil
   :highlight/anchor nil ; [x y]
   :highlight/selected nil ; [color page id] [color page id]
   :pagemark/anchor nil ;  {:left :top :height :edit :page}
   :pagemark/sidebar '() ; {:start-page :end-page :deadline nil/date}
   :theme/dark true ;---
   :pdf nil
   :pdf/viewer nil
   :pdf/pages 37
   :pdf/width "1000px"
   :pdf/loading? false
   :pdf/highlights {} ; {:color :opacity :x0 :y0 :x1 :y1}
   :pdf/pagemarks {} ; {:page-id {:done nil/{:width :height} :skip :schedule}}
   :pagemark? false
   :loading/progress 0})

(def schema
  {;;; Pdf
   :pdf/uid {:db/unique :db.unique/identity
             :db/doc "The identifier for the pdf"}
   :pdf/title {:db/cardinality :db.cardinality/one
               :db/type :db.type/string
               :db/doc "The title of the pdf"}
   :pdf/no-pages {:db/cardinality :db.cardinality/one
                  :db/type :db.type/long
                  :db/doc "The number of pages in the pdf, 1-indexed"}
   :pdf/active-page {:db.cardinality :db.cardinality/one
                     :db/type :db.type/long
                     :db/doc "The page that was last active when the pdf was closed.
                              Will be between 1 and no-pages"}
   :pdf/storage-path {:db.cardinality :db.cardinality/one
                      :db/type :db.type/string
                      :db/doc "The path to the pdf in the filesystem e.g.
                              \\Path\\To\\Pdf"}
  ;;  :pdf/notes

   ;;; Pagemark
   ;;; If the pagemark has none of the attributes /deadline, /end-page-width and /end-page-height
   ;;; its type is :skip
   :pagemark/uid {:db/unique :db.unique/identity
                  :db/doc "The identifier for the pagemark"}
   :pagemark/pdf {:db/cardinality :db.cardinality/one
                  :db/type :db.type/ref
                  :db/doc "Reference to the pdf the pagemark belongs to"}
   :pagemark/type {:db/cardinalit :db.cardinality/one
                   :db/type :db.type/keyword
                   :db/doc "The type of pagemark. One of :done, :skip and :deadline.
                            If :done -> :end-page-width and end-page-height exists.
                            If :deadline -> :pagemark/deadline exists.
                            Else :skip, neither of the above mentioned attributes exists"}
   :pagemark/deadline {:db/cardinality :db.cardinality/one
                       :db/type :db.type/string
                       :db/doc "Either date in format 'YYYY-MM-DD' or ''.  
                                If the pagemark has this attribute its type is :deadline"}
   :pagemark/start-page {:db/cardinality :db.cardinality/one
                         :db/type :db.type/long
                         :db/doc "Page on which the pagemark starts"}
   :pagemark/end-page {:db/cardinality :db.cardinality/one
                       :db/type :db.type/long
                       :db/doc "Page on which the pagemark ends"}
   :pagemark/end-page-width {:db/cardinality :db.cardinality/one
                              :db/type :db.type/float
                              :db/doc "The quotent of the width that's covered
                                       by the last page of the pagemark. Between 0 - 1.
                                       If the pagemark has this attribute its type is :done"}
   :pagemark/end-page-height {:db/cardinality :db.cardinality/one
                               :db/type :db.type/float
                               :db/doc "The quotent of the height that's covered
                                       by the last page of the pagemark. Between 0 - 1
                                       If the pagemark has this attribute its type is :done"}

   ;;; Highlight
   :highlight/uid {:db/unique :db.unique/identity
                   :db/doc "The identifier for the highlight"}
   :highlight/pdf {:db/cardinality :db.cardinality/one
                   :db/type :db.type/ref
                   :db/doc "Reference to the pdf the highlight belongs to"}
   :highlight/block {:db/cardinality :db.cardinality/one
                     :db/type :db.type/ref
                     :db/doc "Each highlight will be at the top of a block in the
                              right-sidebar"}
   :highlight/type {:db/cardinality :db.cardinality/one
                    :db/type :db.type/keyword
                    :db/doc "Highlight type which corresponds to the color and also make
                            highlight-templates possible"}
   :highlight/page {:db/cardinality :db.cardinality/one
                    :db/type :db.type/long
                    :db/doc "The page number on which the highlight resides, 1-indexed"}
   :highlight/start-row {:db/cardinality :db.cardinality/one
                         :db/type :db.type/long
                         :db/doc "The row on which the highlight starts, 0-indexed"}
   :highlight/start-offset {:db/cardinality :db.cardinality/one
                            :db/type :db.type/long
                            :db/doc "How many characters that's skipped before the highlight
                                     starts"}
   :highlight/end-row {:db/cardinality :db.cardinality/one
                       :db/type :db.type/long
                       :db/doc "The row on which the highligh ends, 0-indexed"}
   :highlight/end-offset {:db/cardinality :db.cardinality/one
                          :db/type :db.type/long
                          :db/doc "How many characters to highlight before the highlight stops"}})

;; (defonce dsdb (d/create-conn schema))
(defonce dsdb (r/atom (d/empty-db schema)))
