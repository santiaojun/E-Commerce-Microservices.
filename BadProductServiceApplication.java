# Bad Product Service

Product Service with 50% 503 Error Rate - CS6650 Assignment 3 Module 3

## Overview

This service is designed to demonstrate AWS Application Load Balancer's Automatic Target Weights (ATW) feature. Even though the health check always passes, the service returns 503 errors for 50% of create product requests, causing AWS ALB to automatically reduce traffic to this instance.

## API Endpoints

### POST /product

Create a new product.

- **50% Success Rate**: Returns 201 Created with product details
- **50% Failure Rate**: Returns 503 Service Unavailable

**Request Body:**
```json
{
  "name": "Product Name",
  "description": "Product Description",
  "price": 99.99,
  "quantity": 100
}
```

**Success Response (201):**
```json
{
  "productId": 1,
  "name": "Product Name",
  "description": "Product Description",
  "price": 99.99,
  "quantity": 100
}
```

**Error Response (503):**
```json
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "Service temporarily unavailable. Please try again later."
}
```

### GET /products/{productId}

Get product by ID. Normal functionality - always works.

**Success Response (200):**
```json
{
  "productId": 1,
  "name": "Product Name",
  "description": "Product Description",
  "price": 99.99,
  "quantity": 100
}
```

**Error Response (404):**
```json
{
  "error": "NOT_FOUND",
  "message": "Product with ID {id} not found"
}
```

### GET /health

Health check endpoint. **Always returns 200 OK** (so ALB doesn't mark it as unhealthy).

**Response (200):**
```json
{
  "status": "UP",
  "service": "bad-product-service",
  "totalProducts": 42,
  "errorRate": 0.5
}
```

## Running Locally

### Option 1: Using Maven

```bash
cd bad-product-service
mvn spring-boot:run
```

### Option 2: Run Packaged JAR

```bash
mvn clean package
java -jar target/bad-product-service-1.0.0.jar
```

### Option 3: Using Docker

```bash
docker build -t bad-product-service .
docker run -p 8080:8080 bad-product-service
```

## Testing

### Test create product (run multiple times to see 201 and 503)

```bash
curl -X POST http://localhost:8080/product \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Product","description":"Test Description","price":29.99,"quantity":50}'
```

### Test get product

```bash
curl http://localhost:8080/products/1
```

### Test health check

```bash
curl http://localhost:8080/health
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# Error rate (default 50% = 0.5)
product.service.error-rate=0.5
```

## Purpose

This service demonstrates AWS ALB Automatic Target Weights (ATW):
- Health checks pass (/health always returns 200)
- But 50% of requests return 503 errors
- ALB ATW detects the high error rate
- ALB automatically reduces traffic to this instance
- More traffic goes to healthy instances

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Maven 3.8+

## Author

Runxin Shao - Module 3
