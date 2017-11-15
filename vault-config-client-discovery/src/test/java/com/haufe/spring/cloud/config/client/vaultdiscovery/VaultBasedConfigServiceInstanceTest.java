package com.haufe.spring.cloud.config.client.vaultdiscovery;

import com.google.common.base.Throwables;
import javaslang.control.Try;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static com.haufe.testutils.hamcrest.TryMatcher.tryFailedAndCauseMatches;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link VaultBasedConfigServiceInstance}
 */
public class VaultBasedConfigServiceInstanceTest {

    private static final String SERVICE_ID = "myCustomId";

    private static final URI HTTP_LOCALHOST_9400 = createURI("http://localhost:9400/");
    private static final URI HTTP_LOCALHOST_DEFAULT_PORT = createURI("http://localhost/");
    private static final URI HTTP_LOCALHOST_80 = createURI("http://localhost:80/");
    private static final URI HTTPS_LOCALHOST_9400 = createURI("https://localhost:9400/");
    private static final URI HTTPS_LOCALHOST_9400_CONTEXT = createURI("https://localhost:9400/context");
    private static final URI SFTP_LOCALHOST_9400 = createURI("sftp://localhost:9400/");
    private static final URI HTTPS_VAULT_9400 = createURI("https://vault.project.example.com:9400/");
    private static final URI HTTPS_VAULT_9400_SEGMENT_A =
            createURI("https://vault.project.example.com:9400/segment%20a/");
    private static final URI HTTPS_VAULT_DEFAULT_PORT = createURI("https://vault.project.example.com/");
    private static final URI HTTPS_10_2_5_82_9400 = createURI("https://10.2.5.82:9400/");

    private static final String USERNAME = "peter";
    private static final String PASSWORD = "p3t3r";
    private static final String CONFIG_PATH = "/config/path";

    private static URI createURI(String uriString) {
        try {
            return new URI(uriString);
        }
        catch (URISyntaxException uriException) {
            throw Throwables.propagate(uriException);
        }
    }

    @Test
    public void testThatServiceIdIsPassedThrough() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getServiceId(), equalTo(SERVICE_ID));
    }

    @Test
    public void testThatHttpUrisAreNotSecure() {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.isSecure(), equalTo(false));
    }

    @Test
    public void testThatHttpsUrisAreSecure() {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_LOCALHOST_9400_CONTEXT,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.isSecure(), equalTo(true));
    }

    @Test
    public void testThatNonHttpUrisAreRejectedAsIllegalArgument() {
        Try<VaultBasedConfigServiceInstance> result = Try.of(
                () -> new VaultBasedConfigServiceInstance(SERVICE_ID, SFTP_LOCALHOST_9400,
                                                          USERNAME, PASSWORD,
                                                          null));
        assertThat(result, tryFailedAndCauseMatches(instanceOf(IllegalArgumentException.class)));
    }

    @Test
    public void testThatFqdnHostsAreExtractedFromUri() {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_VAULT_9400_SEGMENT_A,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getHost(), equalTo("vault.project.example.com"));
    }

    @Test
    public void testThatLocalhostIsExtractedFromUri() {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_LOCALHOST_9400_CONTEXT,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getHost(), equalTo("localhost"));
    }

    @Test
    public void testThatNumericHostIdsAreExtractedFromUri() {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_10_2_5_82_9400,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getHost(), equalTo("10.2.5.82"));
    }

    @Test
    public void testThatExplicitPortNumbersAreExtractedFromUri() {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_VAULT_9400_SEGMENT_A,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getPort(), equalTo(9400));
    }

    @Test
    public void testThatImplicitHttpPortIsExtractedFromUri() {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_DEFAULT_PORT,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getPort(), equalTo(80));
    }

    @Test
    public void testThatImplicitHttpsPortIsExtractedFromUri() {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_VAULT_DEFAULT_PORT,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getPort(), equalTo(443));
    }

    @Test
    public void testThatNonBlankUsernameIsPassedToMetadata() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getMetadata(), hasEntry("user", USERNAME));
    }

    @Test
    public void testThatBlankUsernameIsLeftOutFromMetadata() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    " \t\n", PASSWORD, null);
        assertThat(instance.getMetadata(), not(hasKey("user")));
    }

    @Test
    public void testThatEmptyUsernameIsLeftOutFromMetadata() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    "", PASSWORD, null);
        assertThat(instance.getMetadata(), not(hasKey("user")));
    }

    @Test
    public void testThatNullUsernameIsLeftOutFromMetadata() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    null, PASSWORD, null);
        assertThat(instance.getMetadata(), not(hasKey("user")));
    }

    @Test
    public void testThatNonBlankPasswordIsPassedToMetadata() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance.getMetadata(), hasEntry("password", PASSWORD));
    }

    @Test
    public void testThatBlankPasswordIsLeftOutFromMetadata() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, " \t\n", null);
        assertThat(instance.getMetadata(), not(hasKey("password")));
    }

    @Test
    public void testThatEmptyPasswordIsLeftOutFromMetadata() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME,"", null);
        assertThat(instance.getMetadata(), not(hasKey("password")));
    }

    @Test
    public void testThatNullPasswordIsLeftOutFromMetadata() throws URISyntaxException {
        VaultBasedConfigServiceInstance instance =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, null, null);
        assertThat(instance.getMetadata(), not(hasKey("password")));
    }

    @Test
    public void testThatUriIsBaseUri() {
        VaultBasedConfigServiceInstance instance1 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, CONFIG_PATH);
        assertThat(instance1.getUri(), equalTo(HTTP_LOCALHOST_9400));

        VaultBasedConfigServiceInstance instance2 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_DEFAULT_PORT,
                                                    USERNAME, PASSWORD, CONFIG_PATH);
        assertThat(instance2.getUri(), equalTo(HTTP_LOCALHOST_80));

        VaultBasedConfigServiceInstance instance3 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_LOCALHOST_9400_CONTEXT,
                                                    USERNAME, PASSWORD, CONFIG_PATH);
        assertThat(instance3.getUri(), equalTo(HTTPS_LOCALHOST_9400));

        VaultBasedConfigServiceInstance instance4 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_VAULT_9400_SEGMENT_A,
                                                    USERNAME, PASSWORD, CONFIG_PATH);
        assertThat(instance4.getUri(), equalTo(HTTPS_VAULT_9400));
    }

    @Test
    public void testThatRootConfigPathIsOmmitted() {
        VaultBasedConfigServiceInstance instance1 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, null);
        assertThat(instance1.getMetadata(), not(hasKey("configPath")));

        VaultBasedConfigServiceInstance instance2 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, "");
        assertThat(instance2.getMetadata(), not(hasKey("configPath")));

        VaultBasedConfigServiceInstance instance3 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, "/");
        assertThat(instance3.getMetadata(), not(hasKey("configPath")));
    }

    @Test
    public void testThatConfigPathIsCombinedAndDecoded() {
        VaultBasedConfigServiceInstance instance1 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTP_LOCALHOST_9400,
                                                    USERNAME, PASSWORD, CONFIG_PATH);
        assertThat(instance1.getMetadata(), hasEntry("configPath", CONFIG_PATH));

        VaultBasedConfigServiceInstance instance2 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_LOCALHOST_9400_CONTEXT,
                                                    USERNAME, PASSWORD, CONFIG_PATH);
        assertThat(instance2.getMetadata(), hasEntry("configPath", "/context/config/path"));

        VaultBasedConfigServiceInstance instance3 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_LOCALHOST_9400_CONTEXT,
                                                    USERNAME, PASSWORD, "/");
        assertThat(instance3.getMetadata(), hasEntry("configPath", "/context/"));

        VaultBasedConfigServiceInstance instance4 =
                new VaultBasedConfigServiceInstance(SERVICE_ID, HTTPS_VAULT_9400_SEGMENT_A,
                                                    USERNAME, PASSWORD, CONFIG_PATH);
        assertThat(instance4.getMetadata(), hasEntry("configPath", "/segment a/config/path"));
    }
}
