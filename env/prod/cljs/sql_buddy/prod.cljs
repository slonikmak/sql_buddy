(ns sql-buddy.prod
  (:require [sql-buddy.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
