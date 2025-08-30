package io.github.ozkanpakdil.grepwise.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC-Web support.
 * This class ensures that gRPC-Web is properly enabled and configured.
 * Most of the configuration is done through application.properties.
 */
@Configuration
@EnableConfigurationProperties
public class GrpcWebConfig {
    // The actual configuration is done through application.properties
    // Spring gRPC auto-configuration will handle the setup based on those properties
}