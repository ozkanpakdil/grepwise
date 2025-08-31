package io.github.ozkanpakdil.grepwise.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures SPA (Single Page Application) fallback so that the built frontend
 * (served from classpath:/static) can handle client-side routes when packaged
 * inside the Spring Boot application.
 *
 * We forward non-API, non-static routes to /index.html.
 */
@Configuration
public class SpaRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Root
        registry.addViewController("/").setViewName("forward:/index.html");

        // Any path that doesn't start with back-end prefixes and has no dot
        registry.addViewController("/{path:^(?!api|actuator|swagger-ui|v3|graphql|grpc|webjars|assets|static).*$}")
                .setViewName("forward:/index.html");

        // Also handle multi-segment paths by matching any remainder after the first segment
        registry.addViewController("/{path:^(?!api|actuator|swagger-ui|v3|graphql|grpc|webjars|assets|static).*$}/**")
                .setViewName("forward:/index.html");
    }
}
