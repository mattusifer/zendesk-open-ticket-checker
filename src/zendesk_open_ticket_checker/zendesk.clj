(ns zendesk-open-ticket-checker.zendesk
  (:require [clojure.data.codec.base64 :as b64]
            [clj-http.client :as client]
            [cheshire.core :as json]))

(def zd-token (atom nil))
(defn get-zendesk-config []
  {:headers {:Authorization (str "Basic " @zd-token)
             :Accept "application/json"}})

(defn get-parsed-zd-response
  [url]
  (json/parse-string (:body (client/get url (get-zendesk-config))) true))

(defn get-user-id
  [email]
  (loop [cur-page (get-parsed-zd-response 
                   "https://rjmetrics.zendesk.com/api/v2/users.json?role[]=admin&role[]=agent")
         users []]
    (if-let [user (some #(when (= email (or (:email %) "")) %) (:users cur-page))]
      (:id user)
      (when (not (nil? (:next_page cur-page)))
        (recur (get-parsed-zd-response (:next_page cur-page))
               (conj users cur-page))))))

(defn get-tickets-assigned-to-user-id
  [user-id]
  (loop [cur-page (get-parsed-zd-response (str "https://rjmetrics.zendesk.com/api/v2/users/" 
                                               user-id "/tickets/assigned.json"))
         tickets []]
    (if (nil? (:next_page cur-page))
      (concat tickets (:tickets cur-page))
      (recur (get-parsed-zd-response (:next_page cur-page))
             (concat tickets (:tickets cur-page))))))

(defn get-comments-for-ticket
  [ticket-id]
  (let [get-comments-from-audits 
        (fn [audits] 
          (loop [audit (first audits)
                 remaining (rest audits)
                 batch-comments []]
            (if (nil? audit)
              batch-comments 
              (recur (first remaining) (rest remaining)
                     (concat batch-comments 
                             (for [event (filter #(and (:public %) (= (:type %) "Comment")) 
                                                 (:events audit))]
                               (assoc {}
                                      :created_at (:created_at audit)
                                      :author_id (:author_id audit))))))))]
    
    (loop [cur-page (get-parsed-zd-response (str "https://rjmetrics.zendesk.com/api/v2/tickets/" 
                                                 ticket-id "/audits.json"))
           comments []]
      (if (nil? (:next_page cur-page))
        (concat comments (get-comments-from-audits (:audits cur-page)))

        (recur (get-parsed-zd-response (:next_page cur-page))
               (concat comments (:tickets cur-page)))))))

(defn get-org-info
  [organization-id]
  (when (and (not (nil? organization-id)) (not (empty? (char-array organization-id))))
    (-> (get-parsed-zd-response (str "https://rjmetrics.zendesk.com/api/v2/organizations/" 
                                     organization-id ".json"))
        :organization)))

(defn agent-made-last-response?
  [user-id comments]
  (= (:author_id (last (sort-by :created_at comments))) user-id ))

(defn get-tickets-that-need-response [user-email token]
  (reset! zd-token (String. (b64/encode (.getBytes token))))

  (let [user-id (get-user-id user-email)
        raw-tickets (filter #(and (= (:status %) "open")
                                  (not (agent-made-last-response? user-id 
                                                                  (get-comments-for-ticket (:id %))))) 
                            (get-tickets-assigned-to-user-id user-id))]
    (map #(assoc % 
                 :cid (-> (get-org-info (:organization_id %)) 
                          :organiation_fields :cid)
                 :org-name (-> (get-org-info (:organization_id %))
                               :name)) raw-tickets)))

(comment

  (reset! zd-token (String. (b64/encode (.getBytes (slurp "token")))))

  (def user-id (get-user-id "musifer@rjmetrics.com"))

  (def raw-tickets (get-tickets-assigned-to-user-id user-id))

  (def info (get-org-info (:organization_id (first all-tickets))))

)
