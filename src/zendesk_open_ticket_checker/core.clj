(ns zendesk-open-ticket-checker.core
  (:gen-class)
  (:require [zendesk-open-ticket-checker.zendesk :as zd]
            [zendesk-open-ticket-checker.org-mode-editor :as org]
            [clojure.tools.cli :as cmd]))

(defn -main
  "I do a whole lot"
  [& args]
  (let [cli-opts [[nil "--tickets ID" "Get tickets to respond to based on your email address."
                   :validate [#(re-matches #".*@.*\..*" %) "Must be a valid email address."]]]
        error-msg (fn [errors]
                    (str "The following errors occurred while parsing your command: \n\n"
                         (clojure.string/join \newline errors)))
        exit (fn [status msg]
               (println msg)
               (System/exit status))
        usage-summary (fn [summary]
                        (->> ["Use this program to check which tickets you need to respond to."
                              ""
                              "Usage: zd-ticket-checker [options]"
                              ""
                              "Options:"
                              summary]
                             (clojure.string/join \newline)))

        ;; parsed options
        {:keys [options arguments errors summary] :as res-map} (cmd/parse-opts args cli-opts)]
    
    ;; handle errors
    (cond 
      (> (count (keys options)) 1) (exit 1 (error-msg ["Too many options! One task at a time, please."]))
      errors (exit 1 (error-msg errors)))
    
    ;; main
    (case (first (keys options))
      :tickets
      (let [tickets-need-resp (zd/get-tickets-that-need-response (:tickets options))]
        (org/write-new-todo tickets-need-resp)))))

(comment

 (-main "--tickets" "musifer@rjmetrics.com")

)
