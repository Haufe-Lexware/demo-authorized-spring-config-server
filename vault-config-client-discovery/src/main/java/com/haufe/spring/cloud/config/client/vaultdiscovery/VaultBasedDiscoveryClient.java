package com.haufe.spring.cloud.config.client.vaultdiscovery;

import com.google.common.base.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A Spring Cloud Config {@link DiscoveryClient} that reads connection settings from Vault (with the settings in the
 * bootstrap configuration as fall-back).
 */
public class VaultBasedDiscoveryClient implements DiscoveryClient {

    private static final Logger LOG = LoggerFactory.getLogger(VaultBasedDiscoveryClient.class);

    /**
     * The {@link ServiceInstance#getServiceId() service id} of the Spring Cloud Config service.
     *
     * @see #getServices()
     * @see #getInstances(String)
     */
    public static final String CONFIG_SERVICE_ID = ConfigClientProperties.Discovery.DEFAULT_CONFIG_SERVER;

    /**
     * The property name used to look up the config server URI in Vault.
     * <p>
     * It is the same property name that also corresponds to {@link ConfigClientProperties#getUri()}.
     */
    public static final String URI_PROPERTY_NAME = fullPropertyName("uri");

    /**
     * The property name used to look up the config server username in Vault.
     * <p>
     * It is the same property name that also corresponds to {@link ConfigClientProperties#getUsername()}.
     */
    public static final String USERNAME_PROPERTY_NAME = fullPropertyName("username");

    /**
     * The property name used to look up the config server password in Vault.
     * <p>
     * It is the same property name that also corresponds to {@link ConfigClientProperties#getPassword()}.
     */
    public static final String PASSWORD_PROPERTY_NAME = fullPropertyName("password");

    /**
     * The property name used to look up the config server config path in Vault.
     * <p>
     * There is no corresponding property in {@link ConfigClientProperties}; specifying a
     * {@link ConfigClientProperties#getUri() uri} with at least one path segment has the same effect as configuring
     * a config path, though. (In fact, {@link VaultBasedConfigServiceInstance} trimms all path segment from the URI
     * and adds them to the front of the config path.)
     */
    public static final String CONFIG_PATH_PROPERTY_NAME = fullPropertyName("configPath");

    private final ConfigClientProperties configClientProperties;
    private final PropertySourceLocator vaultPropertySourceLocator;
    private final Environment environment;

    /**
     * Constructs a new {@link DiscoveryClient} that reads connections settings for a Spring Cloud Config Server client
     * from vault.
     *
     * @param configClientProperties
     *         the config server client settings found in the bootstrap environment
     * @param vaultPropertySourceLocator
     *         strategy object for reading properties from Vault
     * @param environment
     *         the current environment
     */
    public VaultBasedDiscoveryClient(ConfigClientProperties configClientProperties,
                                     PropertySourceLocator vaultPropertySourceLocator,
                                     Environment environment) {
        Objects.requireNonNull(configClientProperties, "configClientProperties must not be null");
        this.configClientProperties = configClientProperties;

        Objects.requireNonNull(vaultPropertySourceLocator, "vaultPropertySourceLocator must not be null");
        this.vaultPropertySourceLocator = vaultPropertySourceLocator;

        Objects.requireNonNull(environment, "environment must not be null");
        this.environment = environment;
    }

    /**
     * Human-readable description of this {@link DiscoveryClient}
     *
     * @return a {@link StringUtils#isNotBlank(CharSequence) non-blank} description string
     */
    @Override
    public String description() {
        return "Vault-based Discovery Client";
    }

    /**
     * No-op function since there is no special "local" service instance.
     *
     * @deprecated this method was deprecated in Spring Cloud
     *
     * @return always {@code null}
     */
    @Override
    @Deprecated
    public ServiceInstance getLocalServiceInstance() {
        return null;
    }

    /**
     * Obtain a (possibly cached) instance of the {@link VaultBasedConfigServiceInstance}.
     *
     * @param serviceId
     *         the id of the service a discovery client is requested for
     * @return a non-empty list if and only if {@code serviceId} is equal to {@link #CONFIG_SERVICE_ID},
     * never {@code null}
     */
    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        VaultBasedConfigServiceInstance serviceInstance = null;
        if(CONFIG_SERVICE_ID.equals(serviceId)) {
            serviceInstance = createServiceInstance();
        }
        return serviceInstance != null ?
                Collections.singletonList(serviceInstance) :
                Collections.emptyList();
    }

    /**
     * The list of identifiers of services supported by this discovery client.
     *
     * @return a list with {@link #CONFIG_SERVICE_ID} as its only entry
     */
    @Override
    public List<String> getServices() {
        return Collections.singletonList(CONFIG_SERVICE_ID);
    }


    private VaultBasedConfigServiceInstance createServiceInstance() {
        PropertySource<?> vaultPropertySource = vaultPropertySourceLocator.locate(environment);
        URI uri = getUri(vaultPropertySource);
        if (uri == null) {
            LOG.warn("did not find any config server URI");
            return null;
        }
        String userInfo = uri.getUserInfo();
        String username = getUsername(vaultPropertySource, userInfo);
        String password = getPassword(vaultPropertySource, userInfo);
        String configPath = getVaultProperty(CONFIG_PATH_PROPERTY_NAME, vaultPropertySource, null);
        return new VaultBasedConfigServiceInstance(CONFIG_SERVICE_ID, uri, username, password, configPath);
    }

    static String fullPropertyName(String shortName) {
        return ConfigClientProperties.PREFIX + "." + shortName;
    }

    private URI getUri(PropertySource<?> vaultPropertySource) {
        String uriString =
                getVaultProperty(URI_PROPERTY_NAME, vaultPropertySource, configClientProperties.getUri());
        try {
            return StringUtils.isNotBlank(uriString) ? new URI(uriString) : null;
        }
        catch (URISyntaxException e) {
            LOG.error("invalid config server URI {}", uriString);
            return null;
        }
    }

    private String getUsername(PropertySource<?> vaultPropertySource, String userInfo) {
        String defaultUsername = configClientProperties.getUsername();
        if (userInfo != null) {
            String[] userInfoParts = userInfo.split(":", 2);
            defaultUsername = userInfoParts[0];
        }
        return getVaultProperty(USERNAME_PROPERTY_NAME, vaultPropertySource, defaultUsername);
    }

    private String getPassword(PropertySource<?> vaultPropertySource, String userInfo) {
        String defaultPassphrase = configClientProperties.getPassword();
        if (userInfo != null) {
            String[] userInfoParts = userInfo.split(":", 2);
            if (userInfoParts.length > 1) {
                defaultPassphrase = userInfoParts[1];
            }
        }
        return getVaultProperty(PASSWORD_PROPERTY_NAME, vaultPropertySource, defaultPassphrase);
    }

    private String getVaultProperty(String propertyName, PropertySource<?> vaultPropertySource, String defaultValue) {
        Object property = vaultPropertySource.getProperty(propertyName);
        if (property != null) {
            return property.toString();
        }
        return defaultValue;
    }
}
