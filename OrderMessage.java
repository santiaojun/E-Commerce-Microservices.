locals {
  common_tags = {
    Project = var.project_name
    Course  = "CS6650"
    Managed = "terraform"
  }

  log_group_names = toset([
    "/ecs/${var.project_name}/shopping-cart-service",
    "/ecs/${var.project_name}/credit-card-authorizer",
    "/ecs/${var.project_name}/product-service",
    "/ecs/${var.project_name}/bad-product-service",
    "/ecs/${var.project_name}/warehouse-consumer",
    "/ecs/${var.project_name}/rabbitmq"
  ])
}
