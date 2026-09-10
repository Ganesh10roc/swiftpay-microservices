package com.swiftpay.gateway;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TransactionGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionGatewayApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay Transaction Gateway API")
                        .version("1.0.0")
                        .description("REST API for payment initiation and transaction management")
                        .contact(new Contact()
                                .name("SwiftPay Team")
                                .email("support@swiftpay.com")));
    }
}
