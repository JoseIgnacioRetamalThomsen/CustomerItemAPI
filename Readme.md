# Undertow App - Installation and Testing Guide

## 1. Pre-requisites

Before starting, ensure the following software is installed:

- **Java JDK 21**  
  Make sure `JAVA_HOME` is set and `java -version` prints 21.

- **Maven 3.8.7+**
  Add `mvn` to your `PATH` and check with `mvn -v`.

- **Optional: Docker & Docker Compose**   
  Verify with `docker --version` and `docker-compose --version`.

- Optional: `curl` or Postman to test the APIs.

---

## 2. Project Structure

Clone the repository and check the folder structure:
````
undertow-app/
├── docker/ # Optional: Docker setup
│ ├── Dockerfile
│ └── docker-compose.yml
├── src/
├── target/
├── pom.xml
└── Readme.md
````

---

## Build

Clone

```bash
git clone https://github.com/JoseIgnacioRetamalThomsen/CustomerItemAPI.git
```


From project root:

```bash
mvn clean package -DskipTests
```

Generates:

```
target/undertow-app-1.0.0-SNAPSHOT.jar
```

---

## Run

### Direct Java

```bash
java -jar target/undertow-app-1.0.0-SNAPSHOT.jar
```

- Server starts at `http://localhost:8080`.

### Docker (Optional)

From `docker` folder:

```bash
docker-compose up --build -d
```

- MapDB persisted in `./docker/data`.
- Stop containers:

```bash
docker-compose down
```

---

## API Test Examples

### Customers

```bash
curl -X POST http://localhost:8080/customers \
     -H "Content-Type: application/json" \
     -d '{"name":"Alice","lastName":"Smith","gender":"F","email":"alice@example.com"}'

curl -X GET http://localhost:8080/customers
```

### Items

```bash
curl -X POST http://localhost:8080/items \
     -H "Content-Type: application/json" \
     -d '{"name":"Laptop","size":15,"weight":1200,"color":"Silver"}'

curl -X GET http://localhost:8080/items
```

---


## Test Report

Full test report is available in the GitHub Wiki:

[View Test Report](https://github.com/JoseIgnacioRetamalThomsen/CustomerItemAPI/wiki)


## Cleanup (Optional Docker)

```bash
docker system prune -a --volumes
```

