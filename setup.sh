#!/bin/bash
# Mall Management System - Linux setup
# Installs MySQL (if missing), creates the database + user, updates .env and starts the server.
set -e
cd "$(dirname "$0")"

DB_NAME=mall_db
DB_USER=mall_user
DB_PASS=mall_pass

# 1. Install MySQL if it is not there
if ! command -v mysql > /dev/null; then
  echo "MySQL not found, installing..."
  sudo apt-get update
  sudo apt-get install -y mysql-server
else
  echo "MySQL is already installed"
fi

# 2. Install Java if it is not there
if ! command -v java > /dev/null; then
  echo "Java not found, installing..."
  sudo apt-get install -y default-jdk-headless
fi

# 3. Start MySQL
sudo service mysql start

# 4. Create database, table and user
sudo mysql < schema.sql
sudo mysql -e "CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
               GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';"

# add the 5 sample shops only if the table is empty
if [ "$(sudo mysql -N -e "SELECT COUNT(*) FROM $DB_NAME.Shop")" = "0" ]; then
  sudo mysql < seed.sql
  echo "Added 5 sample shops"
fi

# 5. Update .env
echo "DB_URL=jdbc:mysql://localhost:3306/$DB_NAME?user=$DB_USER&password=$DB_PASS" > .env
echo ".env updated"

# 6. Start the server
java -cp lib/mysql-connector-j-9.1.0.jar Server.java
