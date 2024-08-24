(ns sql-buddy.server
    (:require
     [sql-buddy.handler :refer [app]]
     [config.core :refer [env]]
     [org.httpkit.server :as http-kit])
    (:gen-class))

(defn -main [& args]
  (let [port (or (env :port) 3000)]
    (http-kit/run-server #'app {:port port :join? false})))
