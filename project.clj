(defproject parzival "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               ; org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs "2.11.7"]
                 [reagent "0.10.0"]
                 [re-frame "1.1.2"]
                 [day8.re-frame/tracing "0.6.0"]
                 [garden "1.3.10"]
                 [devcards "0.2.7"]
                 [stylefy "2.2.1"]]

  :plugins [[lein-shadow "0.3.1"]
            
            [lein-shell "0.5.0"]]

  :min-lein-version "2.9.0"

  :jvm-opts ["-Xmx1G"]

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]


  :shadow-cljs {:nrepl {:port 8777}
                
                :builds {:app {:target :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :compiler-options {:output-feature-set :es8
                                                  :infer-externs :auto}
                               :modules {:shared {:entries []
                                                  :preloads [devtools.preload]}
                                                             ; day8.re-frame-10x.preload]}
                                         :app {:init-fn parzival.core/init
                                               :depends-on #{:shared}
                                               :preloads [day8.re-frame-10x.preload]}
                                         :pdf.worker {:init-fn parzival.worker/init
                                                      :depends-on #{:shared}
                                                      :web-worker true}}
                                         ; :pdf.viewer {:init-fn parzival.pdf/init
                                         ;              :depends-on #{:shared}}}
                               :dev {:compiler-options {:closure-defines {re-frame.trace.trace-enabled? true
                                                                          day8.re-frame.tracing.trace-enabled? true}}}
                               :release {:build-options
                                         {:ns-aliases
                                          {day8.re-frame.tracing day8.re-frame.tracing-stubs}}}

                               :devtools {:http-root "resources/public"
                                          :http-port 8280 }}

                         :devcards {:asset-path "js/devcards"
                                    :modules {:shared {:entries []}
                                              :main {:init-fn parzival.devcards/main
                                                     :depends-on #{:shared}}
                                              :pdf.worker {:init-fn parzival.worker/init
                                                           :depends-on #{:shared}
                                                           :web-worker true}}
                                    :compiler-options {:devcards true
                                                       :output-feature-set :es8}
                                    :js-options {:resolve {"devcards-marked" {:target :npm :require "marked"}
                                                           "devcards-syntax-highlighter" {:target :npm :require "highlight.js"}}}
                                    :output-dir "resources/public/js/devcards"
                                    :target :browser}
                         }}
  
  :shell {:commands {"karma" {:windows         ["cmd" "/c" "karma"]
                              :default-command "karma"}
                     "open"  {:windows         ["cmd" "/c" "start"]
                              :macosx          "open"
                              :linux           "xdg-open"}}}

  :aliases {"dev"          ["do" 
                            ["shell" "echo" "\"DEPRECATED: Please use lein watch instead.\""]
                            ["watch"]]
            "watch"        ["with-profile" "dev" "do"
                            ["shadow" "watch" "app" "devcards" "browser-test" "karma-test"]]

            "prod"         ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein release instead.\""]
                            ["release"]]

            "release"      ["with-profile" "prod" "do"
                            ["shadow" "release" "app"]]

            "build-report" ["with-profile" "prod" "do"
                            ["shadow" "run" "shadow.cljs.build-report" "app" "target/build-report.html"]
                            ["shell" "open" "target/build-report.html"]]

            "karma"        ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein ci instead.\""]
                            ["ci"]]
            "ci"           ["with-profile" "prod" "do"
                            ["shadow" "compile" "karma-test"]
                            ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "1.0.2"]
                   [day8.re-frame/re-frame-10x "0.7.0"]
                   [cider/cider-nrepl "0.25.1"]]
    :source-paths ["dev"]}

   :prod {}
   
}

  :prep-tasks [])
