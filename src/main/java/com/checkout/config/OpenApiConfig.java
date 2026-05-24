package com.checkout.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI checkoutOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Secure Checkout Service API")
                .description("""
                    Fault-tolerant checkout service with:
                    - **Idempotent payment APIs** — safe retries without double charges
                    - **State-machine-driven order lifecycle** — enforces valid transitions
                    - **Webhook delivery** — async status notifications with exponential back-off retry
                    - **Circuit breaker** — fails fast when the payment gateway is degraded
                    """)
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Checkout Team")
                    .email("checkout@example.com"))
                .license(new License().name("MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local development")
            ));
    }
}
