package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.annotation.ApiVersion;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * Custom RequestMappingHandlerMapping that processes the ApiVersion annotation
 * and modifies the request mapping paths to include version information.
 * 
 * This handler mapping will transform paths like "/api/users" to "/api/v{version}/users"
 * based on the ApiVersion annotation applied to controllers or methods.
 */
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    private static final String VERSION_PREFIX = "/v";

    /**
     * Customizes the RequestMappingInfo by adding version information to the URL path
     * if an ApiVersion annotation is present on the method or class.
     *
     * @param method the handler method
     * @param handlerType the handler type
     * @return the customized RequestMappingInfo
     */
    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
        
        if (info == null) {
            return null;
        }

        // Look for ApiVersion on the method
        ApiVersion methodAnnotation = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        if (methodAnnotation != null) {
            return createApiVersionInfo(methodAnnotation, info);
        }

        // Look for ApiVersion on the class
        ApiVersion typeAnnotation = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        if (typeAnnotation != null) {
            return createApiVersionInfo(typeAnnotation, info);
        }

        return info;
    }

    /**
     * Creates a new RequestMappingInfo with version information added to the URL path.
     *
     * @param annotation the ApiVersion annotation
     * @param info the original RequestMappingInfo
     * @return a new RequestMappingInfo with version information
     */
    private RequestMappingInfo createApiVersionInfo(ApiVersion annotation, RequestMappingInfo info) {
        int version = annotation.value();
        
        // Get the current patterns from the info
        PatternsRequestCondition patterns = info.getPatternsCondition();
        
        // If patterns is null, we can't version the paths
        if (patterns == null) {
            return info;
        }
        
        // Create new patterns with version prefix
        String[] versionedPaths = patterns.getPatterns().stream()
                .map(pattern -> insertVersionIntoPath(pattern, version))
                .toArray(String[]::new);
        
        // Create a new PatternsRequestCondition with the versioned patterns
        PatternsRequestCondition versionedPatterns = new PatternsRequestCondition(versionedPaths);
        
        // Create a new RequestMappingInfo with the versioned patterns
        return new RequestMappingInfo(
                versionedPatterns,
                info.getMethodsCondition(),
                info.getParamsCondition(),
                info.getHeadersCondition(),
                info.getConsumesCondition(),
                info.getProducesCondition(),
                info.getCustomCondition()
        );
    }

    /**
     * Inserts version information into the URL path.
     * For example, "/api/users" becomes "/api/v1/users" for version 1.
     *
     * @param path the original path
     * @param version the API version
     * @return the path with version information
     */
    private String insertVersionIntoPath(String path, int version) {
        // If path starts with "/api/", insert version after "/api"
        if (path.startsWith("/api/")) {
            return "/api" + VERSION_PREFIX + version + path.substring(4);
        }
        
        // Otherwise, add version prefix to the beginning
        return VERSION_PREFIX + version + path;
    }
}