CREATE DATABASE IF NOT EXISTS mall_db;
USE mall_db;

CREATE TABLE IF NOT EXISTS Shop (
  shop_id INT AUTO_INCREMENT PRIMARY KEY,
  shop_name VARCHAR(100),
  owner_name VARCHAR(100),
  floor_no INT,
  rent DECIMAL(10, 2)
);
