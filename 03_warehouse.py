# Credit Card Authorizer Service

Credit Card Authorization Microservice - CS6650 Assignment 3 Module 3

## Overview

This service provides credit card authorization validation, accepting credit card numbers and returning authorization results.

## API Endpoint

### POST /credit-card-authorizer/authorize

Authorize credit card payment.

**Request Body:**
```json
{
  "credit_card_number": "1234-5678-9012-3456"
}
```

**Responses:**

- **200 OK** (90% probability) - Authorization successful
  ```json
  {
    "status": "Authorized"
  }
  ```

- **402 Payment Required** (10% probability) - Payment declined
  ```json
  {
    "error": "DECLINED",
    "message": "Payment declined"
  }
  ```

- **400 Bad Request** - Invalid credit card format
  ```json
  {
    "error": "INVALID_FORMAT",
    "message": "Invalid credit card format. Expected format: XXXX-XXXX-XXXX-XXXX"
  }
  ```

## Running Locally

### Option 1: Using Maven

```bash
cd credit-card-authorizer
mvn spring-boot:run
```

### Option 2: Run Packaged JAR

```bash
mvn clean package
java -jar target/credit-card-authorizer-1.0.0.jar
```

### Option 3: Using Docker

```bash
docker build -t credit-card-authorizer .
docker run -p 8082:8082 credit-card-authorizer
```

## Testing

### Test with valid credit card number (run multiple times to see different results)

```bash
curl -X POST http://localhost:8082/credit-card-authorizer/authorize \
  -H "Content-Type: application/json" \
  -d "{\"credit_card_number\": \"1234-5678-9012-3456\"}"
```

### Test with invalid format (should return 400)

```bash
curl -X POST http://localhost:8082/credit-card-authorizer/authorize \
  -H "Content-Type: application/json" \
  -d "{\"credit_card_number\": \"1234567890123456\"}"
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8082

# Authorization approval rate (default 90%)
creditcard.authorization.approval-rate=0.9
```

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Maven 3.8+

## Author

Runxin Shao - Module 3
