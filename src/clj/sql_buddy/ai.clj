(ns sql-buddy.ai
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [wkok.openai-clojure.api :as api]))

(defn read-resource-file [filename]
  (slurp (io/resource filename)))

(def default-messages
  {:model "gpt-3.5-turbo"
   :messages [{:role "system" :content  (read-resource-file "main-prompt.txt")}]})

(def main-conversation
  (atom default-messages))

(def new-task-msg {:role "user" :content ":new-task"})

(def current-task
  (atom nil))

(def sql-generating-prompt {:model "gpt-3.5-turbo"
                            :messages [{:role "system" :content (read-resource-file "sql-script-prompt.txt")}]})

(def user-msg-template {:role "user"})

(defn build-user-message [msg]
  "Returns a wrapped string message by"
  (assoc user-msg-template :content msg))

(defn build-user-question-msg-content [msg]
  (str ":question " msg))

(defn update-main-conversation [new-message]
  (swap! main-conversation update :messages conj new-message))

(defn get-first-message [answer]
  (-> answer :choices first :message))

(defn send-openai-request [msg]
  "msg should be a valid message with keys :role and :content"
  (let [answer (api/create-chat-completion msg)
        answer-msg (get-first-message answer)]
    answer-msg))

(defn send-main-request [msg]
  (let [answer-msg (send-openai-request msg)]
    (update-main-conversation answer-msg)
    answer-msg))


(defn request-task []
  (let [new-task (send-main-request (update-main-conversation new-task-msg))]
    (reset! current-task new-task)))

(defn send-sql-generation-request []
  (if @current-task
    (let [user-msg (build-user-message (:content @current-task))
          prompt (assoc sql-generating-prompt :messages (conj (:messages sql-generating-prompt) user-msg))]
      (send-openai-request prompt))
    (println "Warning: No current task available.")))

(defn send-user-message [msg]
  (->
    msg
    build-user-message
    update-main-conversation
    send-main-request))

(defn send-answer [answer]
  (->
    answer
    send-user-message))

(defn send-question [question]
  (->
    question
    build-user-question-msg-content
    send-user-message))

(defn get-conversation []
  "Returns array of messages without system role message"
  (rest (:messages @main-conversation)))

(defn print-conversation []
  (doseq [message (get-conversation)]
    (println (str (str/upper-case (:role message)) ": " (:content message)))))



(comment
  (api/create-chat-completion {:model "gpt-3.5-turbo"
                               :messages [{:role "system" :content "You are a helpful assistant."}
                                          {:role "user" :content "Who won the world series in 2020?"}
                                          {:role "assistant" :content "The Los Angeles Dodgers won the World Series in 2020."}
                                          {:role "user" :content "Where was it played?"}]}
                              {:api-key ""}))


