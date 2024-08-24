(ns sql-buddy.data-access
  (:require [next.jdbc :as jdbc]))


(def drop-all-tables-query "-- Drop all tables\nDO $$ DECLARE\n    r RECORD;\nBEGIN\n    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP\n        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';\n    END LOOP;\nEND $$;")

(def db-config
  {:dbtype "postgresql"
   :dbname "postgres"
   :host "localhost"
   :user "postgres"
   :password "mysecretpassword"})

(def db (jdbc/get-datasource db-config))

(def q1 "-- Create Tables
           CREATE TABLE categories (
               category_id INT PRIMARY KEY,
               category_name VARCHAR(100)
           );

           CREATE TABLE products (
               product_id INT PRIMARY KEY,
               product_name VARCHAR(100),
               category_id INT,
               price DECIMAL(10, 2),
               FOREIGN KEY (category_id) REFERENCES categories(category_id)
           );

           CREATE TABLE sales (
               product_id INT,
               sale_date DATE,
               quantity INT,
               FOREIGN KEY (product_id) REFERENCES products(product_id)
           );

           -- Insert Test Data
           INSERT INTO categories (category_id, category_name) VALUES
           (1, 'Electronics'),
           (2, 'Clothing');

           INSERT INTO products (product_id, product_name, category_id, price) VALUES
           (1, 'Laptop', 1, 1000.00),
           (2, 'T-shirt', 2, 20.00);

           INSERT INTO sales (product_id, sale_date, quantity) VALUES
           (1, '2022-09-25', 5),
           (2, '2022-09-30', 10);")

(defn exec-query [query]
  (jdbc/execute! db [query]))
