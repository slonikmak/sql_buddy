(ns sql-buddy.app-view
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [cljs.reader :as reader]
            [ajax.core :refer [GET POST]]
            [ajax.edn :refer [edn-request-format
                              edn-response-format]]))


;; App State
(defonce app-state (r/atom {:chat-messages []
                            :sql-script ""
                            :result-html [:span "There is a result"]}))



(defn add-message [type msg]
  (do
    (println (pr-str @app-state))
    (swap! app-state update :chat-messages conj {:type type :msg msg})))


(defn format-sql-result [result]
  (let [table-name (-> (first result) keys first namespace)
        columns (map #(name (key %)) (first result))
        header [:thead
                [:tr (for [col columns]
                       ^{:key col} [:th col])]]
        rows (for [row result]
               ^{:key (get row :employees/employee_id)}
               [:tr (for [val (vals row)]
                      ^{:key val} [:td (str val)])])]
    [:div.table-container
     [:table
      [:caption (str "Table: " table-name)]
      header
      [:tbody rows]]]))

(defn format-sql-text [text]
  (if (str/includes? text "ERROR")
    [:span.error text]
    [:span text]))

;; Websocket

(defn set-sql-result-status [status-text]
  (swap! app-state assoc :result-html [:div.status status-text]))

(defn parse-edn-data [edn-string]
  (reader/read-string edn-string))

(defn remove-system-messages []
  (swap! app-state update :chat-messages (fn [msgs]
                                           (filterv #(not= (:type %) "system") msgs))))
(defn handle-ai-result [data]
  (do
    (remove-system-messages)
    (let [msg (if (nil? data) "The assistants data is missing." data)]
      (swap! app-state update :chat-messages conj {:type "assistant" :msg (:data msg)}))))

(defn handle-ws-response [response]
  (let [data (parse-edn-data response)
        type (:topic data)
        result (:data data)
        result-str (if (vector? result) (format-sql-result result) (format-sql-text result))]
    (case type
      "ai-result" (handle-ai-result data)
      "sql-result" (swap! app-state assoc :result-html result-str))))

(defonce ws-conn (atom nil))

(defn connect-websocket []
  (let [ws-url (str "ws://" (.-hostname js/window.location) ":" (.-port js/window.location) "/ws")
        ws (js/WebSocket. ws-url)]
    (set! (.-onmessage ws) #(handle-ws-response (.-data %)))
    (set! (.-onopen ws) #(println "WebSocket connection opened"))
    (set! (.-onclose ws) #(println "WebSocket connection closed"))
    (reset! ws-conn ws)))

;; Call connect-websocket when the app is initialized
(connect-websocket)

;;;;;;;;


(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text)))

(defn handler [response]
  (.log js/console (str response)))

(defn clear-chat []
  (swap! app-state assoc :chat-messages []))

(defn clear-sql []
  (swap! app-state assoc :sql-script ""))

(defn clear-result []
  (swap! app-state assoc :result-html ""))

(defn get-current-task-handler [response]
  (let [data (second response)
        result (if (nil? data)
                 "There is no current task."
                 data)]
    (add-message "assistant" result)))


(defn add-system-message []
  (add-message "system" "Working..."))

(defn get-task []
  (do
    (add-system-message)
    (POST "/get-task"
          {:error-handler error-handler
           :handler handler
           :format (edn-request-format)
           :response-format (edn-response-format)})))

(defn get-current-task []
  (GET "/get-current-task"
        {:error-handler error-handler
         :handler get-current-task-handler
         :format (edn-request-format)
         :response-format (edn-response-format)}))

(defn fill-db []
  (do
    (set-sql-result-status "Working....")
    (clear-sql)
    (POST "/fill-db"
          {:error-handler error-handler
           :handler handler
           :format (edn-request-format)
           :response-format (edn-response-format)})))

(defn eval-sql []
  (do
    (set-sql-result-status "Working....")
    (POST "/eval-sql"
          {:params {:sql-script (:sql-script @app-state)}
           :error-handler error-handler
           :handler handler
           :format (edn-request-format)
           :response-format (edn-response-format)})))

(defn send-user-message [msg]
  (do
    (add-message "user" msg)
    (POST "/send-user-message"
          {:params {:message msg}
           :error-handler error-handler
           :handler handler
           :format (edn-request-format)
           :response-format (edn-response-format)})))


(defn chat-message [msg]
  (let [type (:type msg)
        msg (:msg msg)]
    [:div.chat-message {:class type}
     [:div.from (case type
                  "user" "User:"
                  "assistant" "Assistant:"
                  "system" "System:")]
     [:div.message msg]]))


(defn chat-window []
  (let [input-value (r/atom "")]
    (fn []
      [:aside.chat-window
       [:div.header
        [:h2 "Chat"]
        [:div
         [:button.clear-btn {:on-click get-current-task} "Get Current Task"]
         [:button.clear-btn {:on-click clear-chat} "Clear"]]]
       [:div.chat-content
        (for [msg (:chat-messages @app-state)]
          (chat-message msg))]
       [:div.chat-buttons
        [:button {:on-click get-task} "New Task"]
        [:button {:on-click #(reset! input-value ":answer ")} "Answer"]
        [:button {:on-click #(reset! input-value ":question ")} "Question"]]
       [:div.chat-input
        [:input {:type "text"
                 :placeholder "Type a message..."
                 :value @input-value
                 :on-change #(reset! input-value (-> % .-target .-value))}]
        [:button {:on-click #(do
                               (send-user-message @input-value)
                               (add-system-message)
                               (reset! input-value ""))} "Send"]]])))


(defn sql-editor []
  [:section.sql-editor
   [:div.header
    [:h2 "SQL Query Editor"]
    [:button.clear-btn {:on-click clear-sql} "Clear"]]
   [:textarea {:placeholder "Write your SQL query here..."
               :value (:sql-script @app-state)
               :on-change #(swap! app-state assoc :sql-script (-> % .-target .-value))}]
   [:div.sql-editor-buttons
    [:button.run-query {:on-click eval-sql} "Run Query"]
    [:button.fill-db {:on-click fill-db} "Fill Database"]]])

(defn results-window []
  [:aside.results-window
   [:div.header
    [:h2 "Results"]
    [:button.clear-btn {:on-click clear-result} "Clear"]]
   [:div.results-content
    ;; Query results will be displayed here
    (:result-html @app-state)]])

(defn container []
  [:div.container
   [chat-window]
   [sql-editor]
   [results-window]])


