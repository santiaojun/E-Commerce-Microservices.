# Terraform Infrastructure (Module 4)

This folder provisions AWS infrastructure for CS6650 Assignment 3:

- VPC + public subnets + internet gateway
- Security groups
- ECS Fargate cluster and services
- RabbitMQ service exposed via dedicated NLB (`:5672` and `:15672`)
- Application Load Balancer with path-based routing
- Target groups configured with ATW-related settings:
  - `load_balancing_algorithm_type = weighted_random`
  - `load_balancing_anomaly_mitigation = on`

## Services Provisioned

- `product-good` ECS service, desired count = 2
- `product-bad` ECS service, desired count = 1
- `shopping-cart-service`
- `credit-card-authorizer`
- `warehouse-consumer`
- `rabbitmq`

Routing rules on ALB listener `:80`:

- `/product*` and `/products*` -> product target group
- `/shopping-cart*` and `/shopping-carts*` -> shopping cart target group
- `/credit-card*` -> credit card target group

## Usage

1. Copy and edit variables:

```bash
cp terraform.tfvars.example terraform.tfvars
```

2. Update image URIs in `terraform.tfvars` to your ECR images.
   If your lab account cannot create IAM roles, set:
   - `existing_ecs_task_execution_role_arn`
   - `existing_ecs_task_role_arn`
   (for AWS Learner Lab, both can point to `arn:aws:iam::<account-id>:role/LabRole`)

3. Initialize and validate:

```bash
terraform init
terraform fmt -recursive
terraform validate
```

4. Plan and apply:

```bash
terraform plan -out plan.tfplan
terraform apply plan.tfplan
```

5. Get ALB endpoint:

```bash
terraform output alb_dns_name
```

## Notes

- `admin_cidr` controls access to RabbitMQ Management UI (port `15672`).
- `terraform output rabbitmq_management_url` returns the RabbitMQ console URL.
- For production use, restrict `admin_cidr` and add HTTPS listener + ACM cert.
- This setup expects container images already pushed to ECR.
