package com.swiftpay.ledger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LedgerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay Ledger Service API")
                        .version("1.0.0")
                        .description("Ledger service for balance transfers and transaction processing")
                        .contact(new Contact()
                                .name("SwiftPay Team")
                                .email("support@swiftpay.com")));
    }
}
