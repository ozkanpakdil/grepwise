package io.github.ozkanpakdil.grepwise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Configuration class for API versioning.
 * This class sets up the infrastructure for URL path-based API versioning.
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    /**
     * Creates a custom request mapping handler that supports API versioning.
     * This handler will process @ApiVersion annotations on controller classes and methods.
     *
     * @return A RequestMappingHandlerMapping that supports API versioning
     */
    @Bean
    public RequestMappingHandlerMapping apiVersionRequestMappingHandlerMapping() {
        ApiVersionRequestMappingHandlerMapping mapping = new ApiVersionRequestMappingHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE); // Higher priority than the default handler mapping
        return mapping;
    }
}