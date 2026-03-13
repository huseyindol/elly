package com.cms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                final String securitySchemeName = "bearerAuth";
                final String tenantSchemeName = "tenantAuth";

                return new OpenAPI()
                                .info(new Info()
                                                .title("Elly CMS API")
                                                .version("1.0")
                                                .description("API documentation for Elly CMS"))
                                .addServersItem(new io.swagger.v3.oas.models.servers.Server()
                                                .url("https://api.huseyindol.com")
                                                .description("Production (HTTPS)"))
                                .addServersItem(new io.swagger.v3.oas.models.servers.Server()
                                                .url("http://localhost:8080")
                                                .description("Local Development"))
                                .addSecurityItem(new SecurityRequirement()
                                                .addList(securitySchemeName)
                                                .addList(tenantSchemeName))
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName,
                                                                new SecurityScheme()
                                                                                .name(securitySchemeName)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description(
                                                                                                "JWT token authentication. Login endpoint'inden token alıp buraya ekleyin."))
                                                .addSecuritySchemes(tenantSchemeName,
                                                                new SecurityScheme()
                                                                                .name("X-Tenant-ID")
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .description(
                                                                                                "Tenant JWT token. /api/v1/tenants/token endpoint'inden alıp buraya ekleyin. Boş bırakılırsa default tenant kullanılır.")));
        }
}
