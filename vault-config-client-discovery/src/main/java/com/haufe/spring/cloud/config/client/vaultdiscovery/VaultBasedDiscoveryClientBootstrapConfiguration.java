package com.haufe.spring.cloud.config.client.vaultdiscovery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Pull the {@link VaultBasedDiscoveryClientAutoConfiguration auto-configuration} of the
 * {@link VaultBasedDiscoveryClient} into the bootstrap configuration.
 * This class is registered in META-INF/spring.factories under the key
 * {@link org.springframework.cloud.bootstrap.BootstrapConfiguration}.
 */
@ConditionalOnClass(ConfigServicePropertySourceLocator.class)
@ConditionalOnProperty(value = "spring.cloud.config.discovery.enabled", matchIfMissing = false)
@Configuration
@Import({VaultBasedDiscoveryClientAutoConfiguration.class })
public class VaultBasedDiscoveryClientBootstrapConfiguration {
}
