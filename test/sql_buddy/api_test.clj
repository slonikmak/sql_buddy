(ns sql-buddy.api-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [sql-buddy.handler :refer :all]
            [sql-buddy.ai :as ai]
            [wkok.openai-clojure.api :as api]
            [sql-buddy.websockets :as websocket]))

(defn reset-defaults []
  (do
    (reset! ai/current-task nil)
    (reset! ai/main-conversation ai/default-messages)))

(defn reset-main-conversation-fixture [f]
  (reset-defaults)
  (f))

(use-fixtures :each reset-main-conversation-fixture)

(deftest test-get-current-task-is-nil
  "Should check that the current task is nil when there is no task"
  (do
    (is (= @ai/main-conversation ai/default-messages))
    (let [response (app (mock/request :get "/get-current-task"))]
      (is (nil? (:data (:body response))))
      (is (= 200 (:status response))))))

(def ai-response
  {:id "chatcmpl-A03wjOPVXuANZ7Pb9hGPixrj2yGoM",
   :object "chat.completion",
   :created 1724578533,
   :model "gpt-3.5-turbo-0125",
   :choices [{:index 0,
              :message {:role "assistant",
                        :content "The 2020 World Series was played at Globe Life Field in Arlington, Texas.",
                        :refusal nil},
              :logprobs nil,
              :finish_reason "stop"}],
   :usage {:prompt_tokens 53, :completion_tokens 17, :total_tokens 70},
   :system_fingerprint nil})

(deftest test-get-new-task
  (let [captured-data (atom nil)]
    (is (= @ai/main-conversation ai/default-messages))
    (with-redefs [api/create-chat-completion (fn [_] ai-response)
                  run-async (fn [f] (f))
                  websocket/broadcast-message (fn [data] (reset! captured-data data))]
      (let [response (app (mock/request :post "/get-task"))
            messages (:messages @ai/main-conversation)]
        (is (= 200 (:status response)))
        (is (= "{:data \"Received\"}" (:body response)))
        (is (some #(= {:role "user", :content ":new-task"} %) messages))
        (is (some #(= {:role "assistant", :content "The 2020 World Series was played at Globe Life Field in Arlington, Texas.", :refusal nil} %) messages))
        (is (= (pr-str {:topic "ai-result", :data "The 2020 World Series was played at Globe Life Field in Arlington, Texas."}) @captured-data))))))

(deftest test-get-new-task-exception
  (let [captured-data (atom nil)]
    (is (= @ai/main-conversation ai/default-messages))
    (with-redefs [api/create-chat-completion (fn [_] (throw (Exception. "Something went wrong")))
                  run-async (fn [f] (f))
                  websocket/broadcast-message (fn [data] (reset! captured-data data))]
      (let [response (app (mock/request :post "/get-task"))
            messages (:messages @ai/main-conversation)]
        (is (= 200 (:status response)))
        (is (= "{:data \"Received\"}" (:body response)))
        (is (some #(= {:role "user", :content ":new-task"} %) messages))
        (is (not (some #(= {:role "assistant", :content "The 2020 World Series was played at Globe Life Field in Arlington, Texas.", :refusal nil} %) messages)))
        (is (= (pr-str {:topic "ai-result", :data "Something went wrong"}) @captured-data))))))