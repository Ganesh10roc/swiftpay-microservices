package com.swiftpay.analytics;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AnalyticsWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsWorkerApplication.class, args);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay Analytics API")
                        .version("1.0.0")
                        .description("Analytics and metrics for payment transactions")
                        .contact(new Contact()
                                .name("SwiftPay Team")
                                .email("support@swiftpay.com")));
    }
}
