package io.github.ozkanpakdil.grepwise.config;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import java.util.List;

/**
 * Factory for creating EnhancedExcludeFilterContextCustomizer instances. This factory is registered in
 * META-INF/spring.factories to enable automatic application of the customizer to all Spring Boot tests.
 *
 * The enhanced version properly handles constructor arguments for mock beans and ensures they're registered with the correct
 * names.
 */
public class ExcludeFilterContextCustomizerFactory implements ContextCustomizerFactory {

    /**
     * Creates a new EnhancedExcludeFilterContextCustomizer if the test class is annotated with @SpringBootTest.
     *
     * @param testClass The test class
     * @param configAttributes The context configuration attributes
     * @return A new EnhancedExcludeFilterContextCustomizer instance, or null if the customizer should not be applied
     */
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        return new EnhancedExcludeFilterContextCustomizer();
    }
}