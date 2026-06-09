package com.stockasticappbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configuration class for Swagger/OpenAPI documentation.
 * Sets up the API documentation with JWT Bearer token authentication support.
 * The Swagger UI is accessible at swagger-ui.html.
 */
@Configuration
public class SwaggerConfig {

	/**
	 * Creates the custom OpenAPI configuration.
	 * Configures the API title and adds a security scheme for JWT authentication.
	 * All endpoints will require Bearer token authentication in the Swagger UI.
	 *
	 * @return The configured OpenAPI instance.
	 */
	@Bean
	OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info().title("Stockastic API Documentation"))
				.addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
				.components(new Components().addSecuritySchemes("BearerAuth", new SecurityScheme()
						.name("BearerAuth")
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}
}