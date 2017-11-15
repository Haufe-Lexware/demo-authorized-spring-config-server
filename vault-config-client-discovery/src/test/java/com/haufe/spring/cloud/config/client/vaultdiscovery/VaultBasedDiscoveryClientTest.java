package com.haufe.spring.cloud.config.client.vaultdiscovery;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.haufe.testutils.hamcrest.StringUtilsPredicateMatcher.isNotBlank;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link VaultBasedDiscoveryClient}.
 * <p>
 * <b>Note:</b> These tests do <em>not</em> interact with any vault instance at all!
 * They rather assume that a vault client like Spring Cloud Vault injected the properties
 * found in the vault into the Spring environment.
 *
 * @see MockVaultPropertySourceLocator
 */
public class VaultBasedDiscoveryClientTest {

    private static final String CONFIGSERVER_SERVICE_ID = "configserver";
    private static final String HTTPS_VAULT_9400 = "https://vault.project.example.com:9400/";
    private static final String HTTPS_VAULT_9400_CONTEXT = "https://vault.project.example.com:9400/context/";
    private static final String HTTPS_USERINFO_VAULT_9400 = "https://idefix:misteltoe@vault.project.example.com:9400/";
    private static final String HTTP_LOCALHOST_9400 = "http://localhost:9400/";

    private Properties mockVaultProperties;
    private MockVaultPropertySourceLocator mockVaultPropertySourceLocator;
    private MockEnvironment environment;
    private ConfigClientProperties configClientProperties;

    @Before
    public void beforeTest() {
        mockVaultProperties = new Properties();
        mockVaultPropertySourceLocator = new MockVaultPropertySourceLocator(mockVaultProperties);
        environment = new MockEnvironment();
        configClientProperties = new ConfigClientProperties(environment);
    }
    
    @Test
    public void testThatDescriptionIsNotBlank() {
        VaultBasedDiscoveryClient discoveryClient =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);
        assertThat(discoveryClient.description(), isNotBlank());
    }

    @Test
    public void testThatServicesIsASingletonListWithConfigServerOnly() {
        VaultBasedDiscoveryClient discoveryClient =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);
        assertThat(discoveryClient.getServices(), is(equalTo(Collections.singletonList(CONFIGSERVER_SERVICE_ID))));
    }

    /**
     * Verify that {@link VaultBasedDiscoveryClient} does not provide a <em>local</em> service instance.
     *
     * @deprecated the getter {@link DiscoveryClient#getLocalServiceInstance()} was deprecated by Spring Cloud
     */
    @SuppressWarnings("deprecation")
    @Test
    @Deprecated
    public void testThatLocalServiceInstanceIsNull() {
        VaultBasedDiscoveryClient discoveryClient =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);
        assertThat(discoveryClient.getLocalServiceInstance(), is(nullValue()));
    }

    @Test
    public void testThatInstancesIsEmptyListForServiceIdsOtherThanConfigserver() {
        VaultBasedDiscoveryClient discoveryClient =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);
        assertThat(discoveryClient.getInstances("search"), is(empty()));
        assertThat(discoveryClient.getInstances(""), is(empty()));
        assertThat(discoveryClient.getInstances("ConfigServer"), is(empty()));
        assertThat(discoveryClient.getInstances(null), is(empty()));
    }

    @Test
    public void testThatConfigServerInstancesIsEmptyListIfNoUriConfigured() {
        configClientProperties.setUri("");
        VaultBasedDiscoveryClient discoveryClient =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);
        assertThat(discoveryClient.getInstances(CONFIGSERVER_SERVICE_ID), is(empty()));
    }

    @Test
    public void testThatVaultPropertyUriOverwritesConfigClientUri() {
        // configure URIs both in the vault properties and in the config client properties
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.URI_PROPERTY_NAME, HTTPS_VAULT_9400);
        configClientProperties.setUri(HTTP_LOCALHOST_9400);
        VaultBasedDiscoveryClient discoveryClient1 =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);

        List<ServiceInstance> instanceList1 = discoveryClient1.getInstances(CONFIGSERVER_SERVICE_ID);
        assertThat(instanceList1, Matchers.not(empty()));
        assertThat(instanceList1.get(0).getUri(), is(equalTo(URI.create(HTTPS_VAULT_9400))));

        // this time the vault properties do not hold any URI, config client properties act as fall-back.
        mockVaultProperties.remove(VaultBasedDiscoveryClient.URI_PROPERTY_NAME);
        VaultBasedDiscoveryClient discoveryClient2 =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);

        List<ServiceInstance> instanceList2 = discoveryClient2.getInstances(CONFIGSERVER_SERVICE_ID);
        assertThat(instanceList2, Matchers.not(empty()));
        assertThat(instanceList2.get(0).getUri(), is(equalTo(URI.create(HTTP_LOCALHOST_9400))));
    }

    @Test
    public void testThatUriUserinfoservesAsFallbackForUsernameAndPassword() {
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.URI_PROPERTY_NAME, HTTPS_USERINFO_VAULT_9400);
        VaultBasedDiscoveryClient discoveryClient =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);

        Map<String, String> metadata = assertInstanceAndGetMetadata(discoveryClient);
        assertThat(metadata, hasEntry("user", "idefix"));
        assertThat(metadata, hasEntry("password", "misteltoe"));

    }

    @Test
    public void testThatVaultPropertyPasswordOverwritesConfigClientPassowrd() {
        // configure URIs both in the vault properties and in the config client properties
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.URI_PROPERTY_NAME, HTTP_LOCALHOST_9400);
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.USERNAME_PROPERTY_NAME, "Obelix");
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.PASSWORD_PROPERTY_NAME, "golden sickle");
        configClientProperties.setUri(HTTPS_USERINFO_VAULT_9400);
        configClientProperties.setUsername("LuciusDestructivus");
        configClientProperties.setPassword("legion");
        VaultBasedDiscoveryClient discoveryClient1 =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);

        Map<String, String> metadata1 = assertInstanceAndGetMetadata(discoveryClient1);
        assertThat(metadata1, hasEntry("password", "golden sickle"));

        // this time the vault properties have no username and password, so the config client properties "win"
        mockVaultProperties.remove(VaultBasedDiscoveryClient.USERNAME_PROPERTY_NAME);
        mockVaultProperties.remove(VaultBasedDiscoveryClient.PASSWORD_PROPERTY_NAME);
        VaultBasedDiscoveryClient discoveryClient2 =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);

        Map<String, String> metadata2 = assertInstanceAndGetMetadata(discoveryClient2);
        assertThat(metadata2, hasEntry("password", "legion"));

    }
    
    @Test
    public void testThatVaultPropertyUsernameOverwritesConfigClientUsername() {
        // configure URIs both in the vault properties and in the config client properties
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.URI_PROPERTY_NAME, HTTP_LOCALHOST_9400);
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.USERNAME_PROPERTY_NAME, "Obelix");
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.PASSWORD_PROPERTY_NAME, "golden sickle");
        configClientProperties.setUri(HTTPS_USERINFO_VAULT_9400);
        configClientProperties.setUsername("LuciusDestructivus");
        configClientProperties.setPassword("legion");
        VaultBasedDiscoveryClient discoveryClient1 =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);

        Map<String, String> metadata1 = assertInstanceAndGetMetadata(discoveryClient1);
        assertThat(metadata1, hasEntry("user", "Obelix"));

        // this time the vault properties have no username and password, so the config client properties "win"
        mockVaultProperties.remove(VaultBasedDiscoveryClient.USERNAME_PROPERTY_NAME);
        mockVaultProperties.remove(VaultBasedDiscoveryClient.PASSWORD_PROPERTY_NAME);
        VaultBasedDiscoveryClient discoveryClient2 =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);

        Map<String, String> metadata2 = assertInstanceAndGetMetadata(discoveryClient2);
        // With spring-cloud-config-client 1.2.2, the returned user was "idefix".
        // (cf. https://github.com/spring-cloud/spring-cloud-config/issues/622).
        assertThat(metadata2, hasEntry("user", "LuciusDestructivus"));
    }

    @Test
    public void testThatVaultConfigPathPropertyIsCombinedWithUriPath() {
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.URI_PROPERTY_NAME, HTTP_LOCALHOST_9400);
        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.CONFIG_PATH_PROPERTY_NAME, "/config/path");

        VaultBasedDiscoveryClient discoveryClient1 =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);
        Map<String, String> metadata1 = assertInstanceAndGetMetadata(discoveryClient1);
        assertThat(metadata1, hasEntry("configPath", "/config/path"));

        mockVaultProperties.setProperty(VaultBasedDiscoveryClient.URI_PROPERTY_NAME, HTTPS_VAULT_9400_CONTEXT);

        VaultBasedDiscoveryClient discoveryClient2 =
                new VaultBasedDiscoveryClient(configClientProperties, mockVaultPropertySourceLocator, environment);
        Map<String, String> metadata2 = assertInstanceAndGetMetadata(discoveryClient2);
        assertThat(metadata2, hasEntry("configPath", "/context/config/path"));
    }

    @Test
    public void testThatPropertyNamesCorrespondToConfigClientProperties() {
        assertThat(VaultBasedDiscoveryClient.fullPropertyName("uri"),
                   is(equalTo(ConfigClientProperties.PREFIX + ".uri")));
        assertThat(VaultBasedDiscoveryClient.fullPropertyName("username"),
                   is(equalTo(ConfigClientProperties.PREFIX + ".username")));
        assertThat(VaultBasedDiscoveryClient.fullPropertyName("password"),
                   is(equalTo(ConfigClientProperties.PREFIX + ".password")));
    }

    private Map<String, String> assertInstanceAndGetMetadata(VaultBasedDiscoveryClient discoveryClient) {
        List<ServiceInstance> instanceList = discoveryClient.getInstances(CONFIGSERVER_SERVICE_ID);
        assertThat(instanceList, Matchers.not(empty()));
        return instanceList.get(0).getMetadata();
    }

    static class MockVaultPropertySourceLocator implements PropertySourceLocator {

        private final Properties properties;

        MockVaultPropertySourceLocator(Properties properties) {
            this.properties = properties;
        }

        @Override
        public PropertySource<?> locate(Environment environment) {
            return new MockPropertySource(MockVaultPropertySourceLocator.class.getSimpleName(),
                                          properties);
        }
    }

}
