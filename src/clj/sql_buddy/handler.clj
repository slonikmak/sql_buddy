(ns sql-buddy.handler
  (:require
    [reitit.ring :as reitit-ring]
    [sql-buddy.middleware :refer [middleware]]
    [hiccup.page :refer [include-js include-css html5]]
    [config.core :refer [env]]
    [sql-buddy.websockets :as websocket]
    [sql-buddy.service :as service]
    ))


(def mount-target
  [:div#app
   [:h2 "Welcome to the SQL-Buddy"]
   [:p "please wait while Figwheel/shadow-cljs is waking up ..."]
   [:p "(Check the js console for hints if nothing exciting happens.)"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name    "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/styles.css" "/css/styles.min.css"))])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))


(defn index-handler
  [_request]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (loading-page)})

(defn fill-db [request]
  (future
    (let [result (service/fill-db)]
      (websocket/broadcast-message (pr-str  {:topic "sql-result" :data result}))))
  {:status  200
   :headers {"Content-Type" "application/edn"}
   :body    (pr-str {:data "Received"})})

(defn prepare-ai-response [response]
  (if (string? response) (str response) (:content response)))

(defn get-task-handler [request]
  (future
    (let [task (service/get-task)
          content (prepare-ai-response task)]
      (websocket/broadcast-message (pr-str  {:topic "ai-result" :data content}))
      (println "Future executed successfully with content:" content)))
  {:status  200
   :headers {"Content-Type" "application/edn"}
   :body    (pr-str {:data "Received"})})

(defn eval-sql-handler [request]
  (let [sql-script (get-in request [:params :sql-script])]
    (future
      (let [sql-result (service/eval-sql sql-script)]
        (websocket/broadcast-message (pr-str {:topic "sql-result" :data sql-result}))))
    {:status  200
     :headers {"Content-Type" "application/edn"}
     :body    {:data sql-script}})
  )

(defn send-user-message-handler [request]
  (let [user-msg (get-in request [:params :message])]
    (future
      (let [answer (service/send-user-message user-msg)]
        (websocket/broadcast-message (pr-str {:topic "ai-result" :data (:content answer)}))))
    {:status  200
     :headers {"Content-Type" "application/edn"}
     :body    {:data user-msg}})
  )

(defn get-current-task-handler [request]
  (let [task (service/get-current-task)]
    {:status  200
     :headers {"Content-Type" "application/edn"}
     :body    {:data task}}))

(def app
  (reitit-ring/ring-handler
    (reitit-ring/router
      [["/" {:get {:handler index-handler}}]
       ["/ws" {:get websocket/websocket-handler}]
       ["/get-task" {:post {:handler get-task-handler}}]
       ["/get-current-task" {:get {:handler get-current-task-handler}}]
       ["/fill-db" {:post {:handler fill-db}}]
       ["/send-user-message" {:post {:handler send-user-message-handler}}]
       ["/eval-sql" {:post {:handler eval-sql-handler}}]])
    (reitit-ring/routes
      (reitit-ring/create-resource-handler {:path "/" :root "/public"})
      (reitit-ring/create-default-handler))
    {:middleware middleware}))

