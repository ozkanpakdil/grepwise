package io.github.ozkanpakdil.grepwise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@SpringBootApplication
@EnableScheduling
public class GrepWiseApplication {
    private static final Logger logger = LoggerFactory.getLogger(GrepWiseApplication.class);

    public static String appName = "GrepWise";

    public static void main(String[] args) {
        SpringApplication.run(GrepWiseApplication.class, args);
    }

    @Bean
    public SchedulingConfigurer schedulingConfigurer() {
        return new SchedulingConfigurer() {
            @Override
            public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
                logger.info("Configuring scheduled tasks");
            }
        };
    }
}
