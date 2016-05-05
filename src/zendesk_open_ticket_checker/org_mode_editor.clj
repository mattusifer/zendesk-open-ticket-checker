(ns zendesk-open-ticket-checker.org-mode-editor
  (:require [clj-time.local :as time]
            [clj-time.format :as time-format]
            [clj-time.coerce :as time-coerce]))

(def work-todo-list-location
  "/Users/mattusifer/Dropbox/symlinks/emacs/org-mode/work.org")

(defn write-new-todo
  [tickets-need-resp]

  ;; backup
  (spit (str work-todo-list-location ".backup" 
             (time-coerce/to-long (time/local-now))) 
        (slurp work-todo-list-location))

  (let [todo-list (clojure.string/split 
                    (slurp work-todo-list-location)
                    #"\n")
        write (fn [file todo])]
    (loop [ticket (first tickets-need-resp)
           remaining (rest tickets-need-resp)
           new-todo-list todo-list]
      (if (nil? ticket)
        (do
          (spit work-todo-list-location "")
          (doseq [line new-todo-list]
            (spit work-todo-list-location (str line "\n") :append true)))

        (let [new-entry
              (str "** TODO Ticket :: " (:id ticket) " " 
                   (subs (:subject ticket) 0 (min (count (:subject ticket)) 30)))
              entry
              (or (some #(when (re-find (re-pattern (str (:id ticket))) %) %) 
                        new-todo-list)
                  new-entry)
              new-tickets-start-index
              (inc 
               (.indexOf new-todo-list 
                         (some #(when (re-find #"Tickets" %) %) 
                               new-todo-list)))
              entry-index (.indexOf new-todo-list entry)
              new-deadline 
              (str "   DEADLINE: <" 
                   (time-format/unparse (time-format/formatter "yyyy-MM-dd E")
                                        (time/local-now)) ">")]
          (if (re-matches #".*TODO.*" entry)
            (recur (first remaining) (rest remaining) new-todo-list)
            (if (= -1 entry-index)
              (let [[before after] (split-at new-tickets-start-index new-todo-list)
                    new-todo (concat before [entry new-deadline] after)]
                (recur (first remaining) (rest remaining) new-todo))
              (let [[before after] (split-at entry-index new-todo-list)
                    new-todo (concat before [new-entry new-deadline] (drop 2 after))]
                (recur (first remaining) (rest remaining) new-todo)))))))))
