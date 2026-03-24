output "alb_dns_name" {
  description = "Public DNS name of the Application Load Balancer."
  value       = aws_lb.app.dns_name
}

output "ecs_cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.this.name
}

output "product_target_group_arn" {
  description = "Target group ARN for product and bad-product services."
  value       = aws_lb_target_group.product.arn
}

output "shopping_cart_target_group_arn" {
  description = "Target group ARN for shopping-cart-service."
  value       = aws_lb_target_group.shopping_cart.arn
}

output "credit_card_target_group_arn" {
  description = "Target group ARN for credit-card-authorizer."
  value       = aws_lb_target_group.credit_card.arn
}

output "rabbitmq_nlb_dns_name" {
  description = "RabbitMQ NLB DNS name."
  value       = aws_lb.rabbitmq.dns_name
}

output "rabbitmq_management_url" {
  description = "RabbitMQ management UI URL."
  value       = "http://${aws_lb.rabbitmq.dns_name}:15672"
}
