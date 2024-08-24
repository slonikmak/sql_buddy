(ns sql-buddy.middleware
  (:require
    [ring.middleware.params :as params]
    [ring.middleware.edn :as rme]
    [prone.middleware :refer [wrap-exceptions]]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(def custom-defaults
  (-> site-defaults
      (assoc-in [:security :anti-forgery] false)))

(def middleware
  [#(wrap-defaults % custom-defaults)
   wrap-exceptions
   wrap-reload
   rme/wrap-edn-params
   params/wrap-params])
