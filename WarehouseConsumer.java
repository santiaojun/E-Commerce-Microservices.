resource "aws_lb" "app" {
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-alb"
  })
}

resource "aws_lb_target_group" "product" {
  name        = "${var.project_name}-tg-product"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.this.id

  load_balancing_algorithm_type     = "weighted_random"
  load_balancing_anomaly_mitigation = "on"

  health_check {
    enabled             = true
    path                = "/health"
    matcher             = "200"
    healthy_threshold   = 2
    unhealthy_threshold = 10
    timeout             = 5
    interval            = 15
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-tg-product"
  })
}

resource "aws_lb_target_group" "shopping_cart" {
  name        = "${var.project_name}-tg-cart"
  port        = 8081
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.this.id

  load_balancing_algorithm_type     = "weighted_random"
  load_balancing_anomaly_mitigation = "on"

  health_check {
    enabled             = true
    path                = "/shopping-carts"
    matcher             = "200-499"
    healthy_threshold   = 2
    unhealthy_threshold = 5
    timeout             = 5
    interval            = 20
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-tg-cart"
  })
}

resource "aws_lb_target_group" "credit_card" {
  name        = "${var.project_name}-tg-cca"
  port        = 8082
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.this.id

  load_balancing_algorithm_type     = "weighted_random"
  load_balancing_anomaly_mitigation = "on"

  health_check {
    enabled             = true
    path                = "/credit-card-authorizer/authorize"
    matcher             = "200-499"
    healthy_threshold   = 2
    unhealthy_threshold = 5
    timeout             = 5
    interval            = 20
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-tg-cca"
  })
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.app.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "application/json"
      message_body = "{\"error\":\"Not Found\"}"
      status_code  = "404"
    }
  }
}

resource "aws_lb_listener_rule" "product" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.product.arn
  }

  condition {
    path_pattern {
      values = ["/product*", "/products*"]
    }
  }
}

resource "aws_lb_listener_rule" "shopping_cart" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 20

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.shopping_cart.arn
  }

  condition {
    path_pattern {
      values = ["/shopping-cart*", "/shopping-carts*"]
    }
  }
}

resource "aws_lb_listener_rule" "credit_card" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 30

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.credit_card.arn
  }

  condition {
    path_pattern {
      values = ["/credit-card*"]
    }
  }
}
