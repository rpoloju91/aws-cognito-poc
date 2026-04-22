package com.aws.cognito.poc.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {


    @Bean
    public OpenAPI museumPromotionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Museum Management API")
                        .description("API for managing museum operations including bookings, events, customers, and administrative services")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Museum Support")
                                .email("support@museum.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
