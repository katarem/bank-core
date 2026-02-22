package com.bytecodes.ms_accounts.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@OpenAPIDefinition(
        info = @Info(
                title = "Bank-Core MS-Account",
                version = "v1.1.0",
                description = "Microservicio para la gestión de cuentas bancarias",
                contact = @Contact(name = "Equipo 02 ByteCodes")
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Local")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
public class OpenApiConfig {
}
