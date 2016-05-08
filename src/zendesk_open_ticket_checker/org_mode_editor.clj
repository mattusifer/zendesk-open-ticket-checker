(ns zendesk-open-ticket-checker.org-mode-editor
  (:require [clj-time.local :as time]
            [clj-time.format :as time-format]
            [clj-time.coerce :as time-coerce]))

(def work-todo-list-location
  "/Users/mattusifer/Dropbox/symlinks/emacs/org-mode/ticket.org")

(defn write-new-todo
  [tickets-need-resp]
  (let [todo-list (clojure.string/split 
                    (slurp work-todo-list-location)
                    #"\n")]
    (loop [ticket (first tickets-need-resp)
           remaining (rest tickets-need-resp)
           new-todo-list todo-list]
      (if (nil? ticket)
        (do
          (spit work-todo-list-location "")
          (doseq [line new-todo-list]
            (spit work-todo-list-location (str line "\n") :append true)))

        (let [new-entry
              (str "** TODO " (or (:cid ticket) "0000") " :: " (:id ticket) " :: "
                   (subs (:subject ticket) 0 (min (count (:subject ticket)) 50)))
              entry
              (or (some #(when (re-find (re-pattern (str (:id ticket))) %) %) 
                        new-todo-list)
                  new-entry)
              entry-index (.indexOf new-todo-list entry)
              new-deadline 
              (str "   DEADLINE: <" 
                   (time-format/unparse (time-format/formatter "yyyy-MM-dd E")
                                        (time/local-now)) ">")]

          ;; if this is not a new ticket and it's already marked TODO
          ;; don't re-add it to the list
          (if (and (not= entry-index -1) (re-matches #".*TODO.*" entry))
            (recur (first remaining) (rest remaining) new-todo-list)
            (if (= -1 entry-index)
              (let [[before after] (split-at 0 new-todo-list)
                    new-todo (concat before [entry new-deadline] after)]
                (recur (first remaining) (rest remaining) new-todo))
              (let [[before after] (split-at entry-index new-todo-list)
                    new-todo (concat before [new-entry new-deadline] (drop 2 after))]
                (recur (first remaining) (rest remaining) new-todo)))))))))
