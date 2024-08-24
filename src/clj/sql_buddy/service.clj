(ns sql-buddy.service
  (:require [sql-buddy.data-access :as db]
            [sql-buddy.ai :as ai]))

(defn get-task []
  (try
    (ai/request-task)
    (catch Exception e
      (println (str (.getMessage e)))
      (.getMessage e))))

(defn get-current-task [] (:content @ai/current-task))


(defn clear-db []
  (db/exec-query db/drop-all-tables-query))

(defn fill-db []
  (try
    (clear-db)
    (db/exec-query  (:content (ai/send-sql-generation-request)))
    "Database is ready"
    (catch Exception e
      (println (str (.getMessage e)))
      (.getMessage e))))


(defn eval-sql [query]
  (try
    (db/exec-query query)
    (catch Exception e
      (println (str (.getMessage e)))
      (.getMessage e))))

(defn answer [answer] (ai/send-answer answer))

(defn question [msg] (ai/send-question msg))

(defn send-user-message [msg]
  (try
    (ai/send-user-message msg)
    (catch Exception e
      (println (str (.getMessage e)))
      (.getMessage e))))