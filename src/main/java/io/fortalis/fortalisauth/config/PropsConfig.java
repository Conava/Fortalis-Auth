package io.fortalis.fortalisauth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers @ConfigurationProperties classes.
 */
@Configuration
@EnableConfigurationProperties(AuthJwtProperties.class)
public class PropsConfig {
}
