package com.ltc.logisticsproject.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI (/swagger-ui.html) və OpenAPI JSON (/v3/api-docs) üçün ümumi
// konfiqurasiya. Hər @RestController avtomatik sənədləşir — heç bir əlavə
// annotasiya tələb olunmur. Burada yalnız iki şey əlavə edilir: layihə
// haqqında ümumi məlumat (başlıq/təsvir) və "Authorize" düyməsi ilə JWT
// Bearer tokenini bir dəfə daxil edib bütün qorunan endpoint-ləri test etmək
// imkanı (bax SecurityConfig#securityFilterChain — "Authorization: Bearer
// <token>" header-i tələb olunur).
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI fleetraOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fleetra Logistics API")
                        .description("Fleetra — yük daşıma/logistika platformasının backend API-si. "
                                + "Müştəri, sürücü, dispetçer və admin rolları üçün sifariş, reys, "
                                + "ödəniş, gömrük, reytinq və söhbət endpoint-lərini əhatə edir.")
                        .version("v1")
                        .contact(new Contact().name("Fleetra")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
