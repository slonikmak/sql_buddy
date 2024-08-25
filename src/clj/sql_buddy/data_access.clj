(ns sql-buddy.data-access
  (:require [next.jdbc :as jdbc]))


(def drop-all-tables-query "-- Drop all tables\nDO $$ DECLARE\n    r RECORD;\nBEGIN\n    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP\n        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';\n    END LOOP;\nEND $$;")

(def db-host (or (System/getenv "DB_HOST") "localhost"))

(def db-config
  {:dbtype "postgresql"
   :dbname "postgres"
   :host db-host
   :user "postgres"
   :password "mysecretpassword"})

(def db (jdbc/get-datasource db-config))


(defn exec-query [query]
  (jdbc/execute! db [query]))
