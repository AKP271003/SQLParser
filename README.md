# MariaDB SQL Validator

A **robust, advanced, and production-ready** SQL validation tool built with Spring Boot and ANTLR. This project is designed specifically for **MariaDB**, and can **parse and validate virtually all SQL constructs** — from simple queries to complex procedures, `DELIMITER` blocks, and everything in between.

## 🚀 Why This Is the Best SQL Validator for MariaDB

✅ Supports **multi-statement** SQL files  
✅ Handles `DELIMITER` declarations for procedures and triggers  
✅ Validates **stored procedures**, **BEGIN...END** blocks, **CASE** expressions, **complex joins**, and more  
✅ Intelligent handling of `INSERT INTO` statements (with or without column definitions)  
✅ Automatically replaces parameterized placeholders (`?`) with dummy values for validation  
✅ Uses a **custom ANTLR-based MariaDB grammar** for deep syntax checking  
✅ Provides **precise error messages with line numbers**  
✅ RESTful API for easy integration with other tools

---

## 📦 API Endpoints

### 🔹 Validate SQL Query (Raw Text)
```http
POST /api/sql-validator/validate-query
Content-Type: text/plain

<SQL query here>
````

**Response:**

* `200 OK` with success message
* `400 Bad Request` if syntax is invalid

---

### 🔹 Validate SQL File (Upload)

```http
POST /api/sql-validator/validate-file
Content-Type: multipart/form-data

file=<upload your .sql file>
```

**Response:**

* `200 OK` with detailed validation report
* `400 Bad Request` if file is empty or invalid
* `500 Internal Server Error` for processing issues

---

## 🛠 How to Use

### Step 1: Build the JAR

```
./mvnw clean package
```

### Step 2: Run Using Script

Create a file named `validate_sql.sh`:

```
#!/bin/bash
java -jar target/validate_sql.jar
```

Make it executable:

```
chmod +x validate_sql.sh
```

Run it:

```
./validate_sql.sh
```

---

## ✅ Sample Output

```
Valid SQL at line 1: DELIMITER //
Valid SQL at line 2: CREATE PROCEDURE test_proc() BEGIN SELECT * FROM users; END //
Valid SQL at line 3: DELIMITER ;
Invalid query at line 4: Mismatch between number of columns (3) and values (2) in INSERT INTO statement
```

---

## 📚 Tech Stack

* Java 17+
* Spring Boot
* Apache Commons Lang
* ANTLR v4
* Custom MariaDB grammar

---

## 📌 Notes

* File uploads are read and split safely using a custom SQL splitter that respects string literals, comment blocks, and `BEGIN...END` structures.
* The tool is suitable for CI/CD pipelines, web integration, and developer tools.
