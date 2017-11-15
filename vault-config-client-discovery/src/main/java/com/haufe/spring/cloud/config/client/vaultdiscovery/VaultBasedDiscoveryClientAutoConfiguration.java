package com.haufe.spring.cloud.config.client.vaultdiscovery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.DiscoveryClientConfigServiceBootstrapConfiguration;
import org.springframework.cloud.vault.config.VaultBootstrapConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} class that creates a
 * {@link DiscoveryClient} for the connection settings of a Spring Cloud Config Server client.
 * <p>
 * This class is registered in META-INF/spring.factories under the key
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration}.
 * However, auto-configuration will take place only if the following conditions are met:
 * <ul>
 * <li>The property {@literal "haufe.cloud.config.vaultDiscovery.enabled"} is not {@literal false}.</li>
 * <li>The property {@literal "spring.cloud.vault.enabled"} is {@literal true}. In this case, the
 * {@link Import imported} {@link VaultBootstrapConfiguration} provides a {@link PropertySourceLocator} bean named
 * {@literal "vaultPropertySourceLocator"}, injected by a {@link Resource} annotation. (The bean's
 * actual type {@code org.springframework.cloud.vault.config.VaultPropertySourceLocator} is package private
 * and can therefore not be referenced to {@link Autowired autowire} the bean by type.)</li>
 * </ul>
 *
 * @see DiscoveryClientConfigServiceBootstrapConfiguration
 */
@Configuration
@ConditionalOnExpression("${haufe.cloud.config.vaultDiscovery.enabled:true} and ${spring.cloud.vault.enabled:true}")
@ConditionalOnMissingBean(VaultBasedDiscoveryClient.class)
@AutoConfigureAfter(RefreshAutoConfiguration.class)
@Import(VaultBootstrapConfiguration.class)
public class VaultBasedDiscoveryClientAutoConfiguration {

    @Resource(name = "vaultPropertySourceLocator")
    private PropertySourceLocator vaultPropertySourceLocator;

    /**
     * Bean factory that instantiates a {@link DiscoveryClient} that reads the Spring Cloud Config Server
     * connection settings from a {@link org.springframework.cloud.vault.config.VaultPropertySourceLocator} or the
     * {@link ConfigClientProperties} found in the bootstrap configuration, in this order.
     *
     * @param configClientProperties
     *         the fall-back config client configuration found in the bootstrap configuration
     * @param environment
     *         the current environment, required to obtain a
     *         {@link org.springframework.core.env.PropertySource PropertySource} from the
     *         {@link PropertySourceLocator}.
     * @return a discovery client with connection settings for a Spring Cloud Config Server client
     */
    @Bean
    DiscoveryClient discoveryClient(@Autowired ConfigClientProperties configClientProperties,
                                    @Autowired Environment environment) {
        return new VaultBasedDiscoveryClient(configClientProperties,
                                             vaultPropertySourceLocator, environment);
    }

}
