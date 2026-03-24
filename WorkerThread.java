data "aws_caller_identity" "current" {}

locals {
  ecs_execution_role_arn = var.existing_ecs_task_execution_role_arn != "" ? var.existing_ecs_task_execution_role_arn : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"
  ecs_task_role_arn      = var.existing_ecs_task_role_arn != "" ? var.existing_ecs_task_role_arn : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"
}

resource "aws_ecs_cluster" "this" {
  name = "${var.project_name}-cluster"

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-cluster"
  })
}

resource "aws_cloudwatch_log_group" "ecs" {
  for_each = local.log_group_names

  name              = each.key
  retention_in_days = 14
  tags              = local.common_tags
}

resource "aws_lb" "rabbitmq" {
  name               = "${var.project_name}-nlb-rmq"
  load_balancer_type = "network"
  internal           = false
  subnets            = aws_subnet.public[*].id

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-nlb-rmq"
  })
}

resource "aws_lb_target_group" "rabbitmq_amqp" {
  name        = "${var.project_name}-tg-rmq-5672"
  port        = 5672
  protocol    = "TCP"
  target_type = "ip"
  vpc_id      = aws_vpc.this.id

  health_check {
    enabled  = true
    protocol = "TCP"
  }
}

resource "aws_lb_target_group" "rabbitmq_mgmt" {
  name        = "${var.project_name}-tg-rmq-15672"
  port        = 15672
  protocol    = "TCP"
  target_type = "ip"
  vpc_id      = aws_vpc.this.id

  health_check {
    enabled  = true
    protocol = "TCP"
  }
}

resource "aws_lb_listener" "rabbitmq_amqp" {
  load_balancer_arn = aws_lb.rabbitmq.arn
  port              = 5672
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.rabbitmq_amqp.arn
  }
}

resource "aws_lb_listener" "rabbitmq_mgmt" {
  load_balancer_arn = aws_lb.rabbitmq.arn
  port              = 15672
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.rabbitmq_mgmt.arn
  }
}

resource "aws_ecs_task_definition" "rabbitmq" {
  family                   = "${var.project_name}-rabbitmq"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.rabbitmq_cpu
  memory                   = var.rabbitmq_memory
  execution_role_arn       = local.ecs_execution_role_arn
  task_role_arn            = local.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "rabbitmq"
      image     = var.rabbitmq_image
      essential = true
      environment = [
        { name = "RABBITMQ_DEFAULT_USER", value = "guest" },
        { name = "RABBITMQ_DEFAULT_PASS", value = "guest" }
      ]
      portMappings = [
        { containerPort = 5672, hostPort = 5672, protocol = "tcp" },
        { containerPort = 15672, hostPort = 15672, protocol = "tcp" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs["/ecs/${var.project_name}/rabbitmq"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_task_definition" "shopping_cart" {
  family                   = "${var.project_name}-shopping-cart"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.shopping_cart_cpu
  memory                   = var.shopping_cart_memory
  execution_role_arn       = local.ecs_execution_role_arn
  task_role_arn            = local.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "shopping-cart-service"
      image     = var.shopping_cart_image
      essential = true
      environment = [
        { name = "SERVER_PORT", value = "8081" },
        { name = "RABBITMQ_HOST", value = aws_lb.rabbitmq.dns_name },
        { name = "RABBITMQ_PORT", value = "5672" },
        { name = "RABBITMQ_USER", value = "guest" },
        { name = "RABBITMQ_PASS", value = "guest" },
        { name = "RABBITMQ_QUEUE", value = "warehouse_queue" }
      ]
      portMappings = [
        { containerPort = 8081, hostPort = 8081, protocol = "tcp" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs["/ecs/${var.project_name}/shopping-cart-service"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_task_definition" "credit_card" {
  family                   = "${var.project_name}-credit-card-authorizer"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.credit_card_cpu
  memory                   = var.credit_card_memory
  execution_role_arn       = local.ecs_execution_role_arn
  task_role_arn            = local.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "credit-card-authorizer"
      image     = var.credit_card_image
      essential = true
      portMappings = [
        { containerPort = 8082, hostPort = 8082, protocol = "tcp" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs["/ecs/${var.project_name}/credit-card-authorizer"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_task_definition" "product_good" {
  family                   = "${var.project_name}-product-good"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.product_cpu
  memory                   = var.product_memory
  execution_role_arn       = local.ecs_execution_role_arn
  task_role_arn            = local.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "product-service"
      image     = var.product_image
      essential = true
      environment = [
        { name = "SERVER_PORT", value = "8080" },
        { name = "PRODUCT_SERVICE_ERROR_RATE", value = "0.0" }
      ]
      portMappings = [
        { containerPort = 8080, hostPort = 8080, protocol = "tcp" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs["/ecs/${var.project_name}/product-service"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_task_definition" "product_bad" {
  family                   = "${var.project_name}-product-bad"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.product_cpu
  memory                   = var.product_memory
  execution_role_arn       = local.ecs_execution_role_arn
  task_role_arn            = local.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "bad-product-service"
      image     = var.bad_product_image
      essential = true
      environment = [
        { name = "SERVER_PORT", value = "8080" },
        { name = "PRODUCT_SERVICE_ERROR_RATE", value = "0.5" }
      ]
      portMappings = [
        { containerPort = 8080, hostPort = 8080, protocol = "tcp" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs["/ecs/${var.project_name}/bad-product-service"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_task_definition" "warehouse" {
  family                   = "${var.project_name}-warehouse"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.warehouse_cpu
  memory                   = var.warehouse_memory
  execution_role_arn       = local.ecs_execution_role_arn
  task_role_arn            = local.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "warehouse-consumer"
      image     = var.warehouse_image
      essential = true
      environment = [
        { name = "RABBITMQ_HOST", value = aws_lb.rabbitmq.dns_name },
        { name = "RABBITMQ_PORT", value = "5672" },
        { name = "RABBITMQ_USER", value = "guest" },
        { name = "RABBITMQ_PASS", value = "guest" },
        { name = "RABBITMQ_VHOST", value = "/" },
        { name = "QUEUE_NAME", value = "warehouse_queue" },
        { name = "NUM_CONSUMER_THREADS", value = "10" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs["/ecs/${var.project_name}/warehouse-consumer"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_service" "rabbitmq" {
  name            = "${var.project_name}-rabbitmq"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.rabbitmq.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.rabbitmq.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.rabbitmq_amqp.arn
    container_name   = "rabbitmq"
    container_port   = 5672
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.rabbitmq_mgmt.arn
    container_name   = "rabbitmq"
    container_port   = 15672
  }

  depends_on = [
    aws_lb_listener.rabbitmq_amqp,
    aws_lb_listener.rabbitmq_mgmt
  ]

  tags = local.common_tags
}

resource "aws_ecs_service" "product_good" {
  name            = "${var.project_name}-product-good"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.product_good.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.service.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.product.arn
    container_name   = "product-service"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]

  tags = local.common_tags
}

resource "aws_ecs_service" "product_bad" {
  name            = "${var.project_name}-product-bad"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.product_bad.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.service.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.product.arn
    container_name   = "bad-product-service"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]

  tags = local.common_tags
}

resource "aws_ecs_service" "shopping_cart" {
  name            = "${var.project_name}-shopping-cart"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.shopping_cart.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.service.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.shopping_cart.arn
    container_name   = "shopping-cart-service"
    container_port   = 8081
  }

  depends_on = [
    aws_lb_listener.http,
    aws_ecs_service.rabbitmq
  ]

  tags = local.common_tags
}

resource "aws_ecs_service" "credit_card" {
  name            = "${var.project_name}-credit-card-authorizer"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.credit_card.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.service.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.credit_card.arn
    container_name   = "credit-card-authorizer"
    container_port   = 8082
  }

  depends_on = [aws_lb_listener.http]

  tags = local.common_tags
}

resource "aws_ecs_service" "warehouse" {
  name            = "${var.project_name}-warehouse-consumer"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.warehouse.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.service.id]
    assign_public_ip = true
  }

  depends_on = [aws_ecs_service.rabbitmq]

  tags = local.common_tags
}
