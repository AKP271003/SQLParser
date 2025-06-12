#!/bin/bash

SQL_DIR="./sql_queries"
JAR_PATH="./target/SqlAntlr-1.0-SNAPSHOT.jar"

if [ ! -d "$SQL_DIR" ]; then
  echo "Error: SQL directory does not exist."
  exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
  echo "Error: JAR file does not exist."
  exit 1
fi

# Get all .sql files
sql_files=$(find "$SQL_DIR" -type f -name "*.sql")

if [ -z "$sql_files" ]; then
    echo "No SQL files found in the directory."
    exit 1
fi

echo "Validating SQL files in: $SQL_DIR"

# Run the Java program and capture its output with exit status
output=$(java -jar "$JAR_PATH" $sql_files 2>&1)
exit_status=$?

echo "$output"

# Check the exit status
if [ $exit_status -eq 0 ]; then
    echo "Validation successful: All SQL files are valid."
else
    echo "Validation failed: One or more SQL files are invalid."
fi

# Exit with the same status
exit $exit_status