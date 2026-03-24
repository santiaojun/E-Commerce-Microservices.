
This document reflects the **current codebase and runtime behavior** in this repository.

## Team & Module Ownership

| Name | Module | Current Scope in Repo |
|------|--------|-----------------------|
| Yunhong Huang | Module 1 | `shopping-cart-service` + RabbitMQ publisher confirms wiring |
| Qingyu Cheng | Module 2 | `warehouse-consumer` + RabbitMQ consumer behavior |
| Runxin Shao | Module 3 | `credit-card-authorizer` + `bad-product-service` |
| Ziqi Yang | Module 4 | `docker-compose.yml`, Terraform infra, AWS deployment/report operations |

## Contribution Attribution (Detailed)

| Member           | Modules / Tech Used | Concrete Work Delivered |
|------------------|------|------|
| Yunhong Huang    | ShoppingCart Service (`Spring Boot`), RabbitMQ Producer (`RabbitTemplate`) | Implemented cart APIs (`create`, `addItem`, `get`, `checkout`); built checkout order payload (`order_id` + `items`) and published to `warehouse_queue`; configured publisher confirms/returns and message correlation IDs for broker delivery tracking. |
| Qingyu Cheng     | Warehouse Consumer (`com.rabbitmq.client`), concurrency (`Thread`, `AtomicInteger`, `ConcurrentHashMap`) | Built multi-thread RabbitMQ consumer (`NUM_CONSUMER_THREADS`) with per-thread channel model; implemented manual ACK flow (`basicConsume autoAck=false` + `basicAck`) and `basicQos(1)` back-pressure; added shutdown summary output for total orders and per-product quantities, plus queue/env-based broker wiring. |
| Runxin Shao      | Credit Card Authorizer (`Spring Boot`, `jakarta.validation`), Bad Product Service (`Spring Boot`) | Implemented `/credit-card-authorizer/authorize` with card-format validation and configurable approval rate (`creditcard.authorization.approval-rate`, default `0.9`) returning `200/402`; implemented bad product APIs with configurable failure injection (`product.service.error-rate`, default `0.5`) returning `201/503` to demonstrate ALB ATW behavior while keeping `/health` always `200`; kept `load-test-client/` as placeholder in repo for external load testing workflow. |
| Ziqi Yang (Odie) | Infrastructure as Code (`Terraform`), deployment (`AWS ECS/Fargate`, `ALB`, `NLB`), local orchestration (`Docker Compose`) | Implemented Terraform for VPC/public subnets, security groups, ECS cluster/services, RabbitMQ NLB (`5672/15672`), and ALB listener rules for `/product*`, `/shopping-carts*`, `/credit-card*`; configured target groups with ATW-related settings (`weighted_random` + anomaly mitigation `on`) and deployment outputs (ALB/RabbitMQ endpoints); maintained compose-level service wiring/env configuration and delivery documentation/report artifacts. |

## Repository Reality Check

Implemented service folders:
- `shopping-cart-service/`
- `credit-card-authorizer/`
- `bad-product-service/`
- `warehouse-consumer/`
- `infrastructure/terraform/`

Placeholder folders (currently only `.gitkeep`):
- `load-test-client/`

Load testing note:
- `load-test-client/` is intentionally a placeholder in this repo.
- Load testing is run manually using external tooling/scripts.

Important local composition detail:
- `docker-compose.yml` builds one image from `bad-product-service` and runs:
  - `product-good-a` with `PRODUCT_SERVICE_ERROR_RATE=0.0`
  - `product-good-b` with `PRODUCT_SERVICE_ERROR_RATE=0.0`
  - `bad-product-service` with `PRODUCT_SERVICE_ERROR_RATE=0.5`

## Service Contracts (Current Implementation)

### Shopping Cart Service (`shopping-cart-service`)

Base path: `/shopping-carts`

| Method | Path | Parameters | Notes |
|-------|------|------------|-------|
| POST | `/shopping-carts` | query: `customer_id` | Creates cart, returns object containing `cartId` |
| POST | `/shopping-carts/{cartId}/addItem` | query: `product_id`, `quantity` | Adds/increments item |
| GET | `/shopping-carts/{cartId}` | path `cartId` | Returns cart |
| POST | `/shopping-carts/{cartId}/checkout` | query: `credit_card_number` | Publishes order to RabbitMQ if locally authorized |

Current checkout authorization logic in code:
- Uses local method `callCCA(...)` inside controller.
- Authorization succeeds only when `credit_card_number == "1234"`.
- No outbound HTTP call to `credit-card-authorizer` is currently made by shopping-cart service.

RabbitMQ publish behavior:
- Uses Spring `RabbitTemplate`.
- Publisher confirms enabled (`spring.rabbitmq.publisher-confirm-type=correlated`).
- Returns callback enabled (`spring.rabbitmq.publisher-returns=true`).
- Routing to queue from config `rabbitmq.queue` (default `warehouse_queue`).

Order message produced:
```json
{
  "order_id": "uuid-string",
  "items": [
    { "product_id": "1", "quantity": 2 }
  ]
}
```

### Credit Card Authorizer (`credit-card-authorizer`)

Base path: `/credit-card-authorizer`

| Method | Path | Request Body | Behavior |
|-------|------|--------------|----------|
| POST | `/credit-card-authorizer/authorize` | `{"credit_card_number":"1234-5678-9012-3456"}` | Regex validation then random approval/decline |

Behavior details:
- Format validation via regex: `^\d{4}-\d{4}-\d{4}-\d{4}$`.
- Invalid format -> HTTP 400 with `{"error":"INVALID_FORMAT", ...}`.
- Valid format -> random:
  - approve probability from config `creditcard.authorization.approval-rate` (default `0.9`) -> HTTP 200.
  - otherwise -> HTTP 402 with `{"error":"DECLINED", ...}`.

### Bad Product Service (`bad-product-service`)

| Method | Path | Behavior |
|-------|------|----------|
| POST | `/product` | Random fail based on `product.service.error-rate` (default `0.5`), returns 503 on fail |
| GET | `/products/{productId}` | Reads product from in-memory map |
| GET | `/health` | Always 200 (used for ALB health check stability) |

### Warehouse Consumer (`warehouse-consumer`)

Type: standalone Java consumer (not HTTP service).

Queue contract:
- Queue name: `warehouse_queue` (env `QUEUE_NAME` default).
- Declared durable (`queueDeclare(..., true, false, false, null)`).

Consumer behavior:
- Multi-threaded (`NUM_CONSUMER_THREADS`, default `10`).
- Each thread uses its own channel.
- `basicQos(1)` configured.
- `basicConsume(..., autoAck=false, ...)` manual ack mode.
- Ack is sent before JSON parse/stat update (matches current code comments/reference pattern).

Message model:
- `order_id` is parsed as `String` (UUID-compatible).
- `items[].product_id` parsed as `int` (string numerals like `"1"` are accepted by Jackson).
- `items[].quantity` parsed as `int`.

Shutdown output (required evidence):
- Prints:
  - `Total Orders Processed: <n>`
  - `Per-Product Quantities:`
  - `ProductID <id> -> <qty>`

## End-to-End Runtime Logic (Current)

Current path executed in this repo:
1. Client creates cart and adds items via shopping-cart endpoints.
2. Client calls `/shopping-carts/{cartId}/checkout?credit_card_number=1234`.
3. Shopping-cart performs local authorization check (`"1234"` only).
4. On success, shopping-cart publishes order to RabbitMQ with publisher confirms.
5. Warehouse consumer receives message, manual-acks, updates counters.
6. On consumer shutdown, aggregate totals are printed.

Note:
- `credit-card-authorizer` service is implemented and deployable, but shopping-cart does not currently invoke it over HTTP.

## Deployment Topology (Terraform, Current)

Terraform directory: `infrastructure/terraform/`

Provisioned resources:
- VPC, public subnets, route table, IGW
- ALB + listener rules for app paths
- ECS cluster + Fargate services:
  - `product-good` desired count 2
  - `product-bad` desired count 1
  - `shopping-cart`
  - `credit-card-authorizer`
  - `warehouse-consumer`
  - `rabbitmq`
- Dedicated RabbitMQ NLB:
  - TCP 5672 (AMQP)
  - TCP 15672 (Management UI)

Load balancing:
- Product target group attributes:
  - `load_balancing_algorithm_type = weighted_random`
  - `load_balancing_anomaly_mitigation = on`
- Listener routing:
  - `/product*`, `/products*` -> product target group
  - `/shopping-cart*`, `/shopping-carts*` -> shopping-cart target group
  - `/credit-card*` -> credit-card target group

Learner Lab compatibility:
- Terraform defaults to existing IAM role `LabRole` when custom role ARNs are not provided.
- No Cloud Map/Service Discovery dependency in current version.

Important outputs:
- `alb_dns_name`
- `rabbitmq_nlb_dns_name`
- `rabbitmq_management_url`

## Verification Commands (Used During Final Run)

Set service up/down:
```bash
aws ecs update-service --region us-east-1 --cluster cs6650-a3-cluster --service cs6650-a3-warehouse-consumer --desired-count 1 --force-new-deployment
aws ecs update-service --region us-east-1 --cluster cs6650-a3-cluster --service cs6650-a3-warehouse-consumer --desired-count 0
```

Capture final warehouse shutdown stats:
```bash
aws logs tail /ecs/cs6650-a3/warehouse-consumer --since 20m --region us-east-1 | grep -E "Shutdown signal|Total Orders Processed|Per-Product Quantities|ProductID"
```

## Evidence Mapping (for Report)

- ATW enabled: ALB target group attributes screenshot.
- ATW behavior over time: CloudWatch/target-group request distribution screenshots.
- Queue behavior: RabbitMQ `warehouse_queue` graphs (ramp-up + near-zero steady state).
- Warehouse final output: shutdown log lines including totals and per-product quantities.
