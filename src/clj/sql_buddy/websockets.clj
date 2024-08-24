(ns sql-buddy.websockets
  (:require [org.httpkit.server :as httpkit]))

;; websocket

(defonce clients (atom #{}))

(defn on-open [ws]
  (swap! clients conj ws)
  (println "Client connected"))

(defn on-close [ws status]
  (swap! clients disj ws)
  (println "Client disconnected:" status))

(defn websocket-handler [req]
  (httpkit/as-channel req
                      {:on-open    on-open
                       :on-close   on-close}))
(defn broadcast-message [msg]
  (doseq [client @clients]
    (httpkit/send! client msg)))

;;;;;;;;
