package com.cs6650.creditcard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CreditCardAuthorizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditCardAuthorizerApplication.class, args);
        System.out.println("====================================");
        System.out.println("Credit Card Authorizer Service Started");
        System.out.println("Running on port: 8082");
        System.out.println("Endpoint: POST /credit-card-authorizer/authorize");
        System.out.println("====================================");
    }
}
