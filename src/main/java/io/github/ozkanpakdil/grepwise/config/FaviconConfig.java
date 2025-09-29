package io.github.ozkanpakdil.grepwise.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Favicon setup without a controller.
 *
 * We serve favicon.svg from classpath:/static by default and redirect the conventional
 * /favicon.ico path to it. This avoids container error-dispatch recursion when the
 * frontend build isn't present and keeps the security/filter chain simple.
 */
@Configuration
public class FaviconConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/favicon.ico", "/favicon.svg")
                .setStatusCode(HttpStatus.MOVED_PERMANENTLY);
    }
}
