(ns zendesk-open-ticket-checker.core
  (:gen-class)
  (:require [zendesk-open-ticket-checker.zendesk :as zd]
            [zendesk-open-ticket-checker.org-mode-editor :as org]
            [clojure.tools.cli :as cmd]))

(defn -main
  "I do a whole lot"
  [& args]
  (let [cli-opts [["-e" "--email EMAIL" "Get tickets to respond to based on your email address."
                   :validate [#(re-matches #".*@.*\..*" %) "Must be a valid email address."]]
                  ["-t" "--token TOKEN" "Your zendesk API token."]]
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
      (or (empty? options) (not (contains? options :email)))
      (exit 1 (usage-summary summary))
      (not (contains? options :token))
      (exit 1 (error-msg ["You need to supply an API token."]))
      errors (exit 1 (error-msg errors)))
    
    ;; main
    (let [tickets-need-resp (zd/get-tickets-that-need-response (:email options) (:token options))]
      (org/write-new-todo tickets-need-resp))))

(comment

 (-main "--tickets" "musifer@rjmetrics.com")

)
