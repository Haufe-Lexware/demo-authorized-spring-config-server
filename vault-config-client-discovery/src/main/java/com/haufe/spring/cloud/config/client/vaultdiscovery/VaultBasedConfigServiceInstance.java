package com.haufe.spring.cloud.config.client.vaultdiscovery;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of {@link org.springframework.cloud.client.discovery.DiscoveryClient discovered} connection
 * settings for a Spring Cloud Config client.
 */
class VaultBasedConfigServiceInstance implements ServiceInstance {

    private static final Logger LOG = LoggerFactory.getLogger(VaultBasedConfigServiceInstance.class);
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final ImmutableMap<String, Integer> WELL_KNOWN_URI_SCHEMES =
            ImmutableMap.<String, Integer>builder()
                    .put(HTTP_SCHEME, 80)
                    .put(HTTPS_SCHEME, 443)
                    .build();

    private final String configServiceId;
    private final String host;
    private final int port;
    private final URI baseUri;
    private final boolean secure;
    private final ImmutableMap<String, String> metadata;

    /**
     * Construct a new {@link ServiceInstance} representation.
     *
     * @param configServiceId
     *         the identifier of the service, must not be {@literal null}
     * @param uri
     *         the locator where the service can be accessed; the URI must not be {@literal null} and its
     *         {@link URI#getScheme() scheme} must be either {@literal "http"} or {@literal "https"}
     * @param username
     *         the HTTP basic auth username, might be {@link StringUtils#isBlank(CharSequence) blank}
     * @param password
     *         the HTTP basic auth password, might be {@link StringUtils#isBlank(CharSequence) blank}
     * @param configPath
     *         the config path to be appended to the URI, might be {@link StringUtils#isBlank(CharSequence) blank}
     */
    VaultBasedConfigServiceInstance(String configServiceId, URI uri,
                                    String username, String password,
                                    String configPath) {
        Objects.requireNonNull(configServiceId, "configServiceId must not be null");
        this.configServiceId = configServiceId;

        Objects.requireNonNull(uri, "uri must not be null");

        String scheme = uri.getScheme();
        this.secure = isSchemeSecure(scheme);
        this.host = uri.getHost();
        int uriPort = uri.getPort();
        this.port = uriPort > 0 ?
                    uriPort :
                    WELL_KNOWN_URI_SCHEMES.getOrDefault(uri.getScheme(), -1);
        this.baseUri = createBaseUri(scheme, this.host, this.port);
        String completeConfigPath = createCombinedConfigPath(uri, configPath);
        metadata = buildMetadata(username, password, completeConfigPath);
    }

    private String createCombinedConfigPath(URI uri, String configPath) {
        URI completeUri = StringUtils.isEmpty(configPath) ?
                          uri :
                          UriComponentsBuilder
                                  .fromUri(uri)
                                  .path(configPath)
                                  .build(true)
                                  .toUri();
        String completePath = completeUri.getPath();
        return "/".equals(completePath) ? null : completePath;
    }

    private URI createBaseUri(String scheme, String host, int port) {
        try {
            return new URI(scheme, null, host, port, "/", null, null);
        }
        catch (URISyntaxException uriSyntaxException) {
            throw new ConfigServerDiscoveryException("could not construct the service's base URI", uriSyntaxException);
        }
    }

    private ImmutableMap<String, String> buildMetadata(String username, String password, String configPath) {
        ImmutableMap.Builder<String, String> metadataBuilder = ImmutableMap.builder();
        if (StringUtils.isNotBlank(username)) {
            metadataBuilder.put("user", username);
        }
        if (StringUtils.isNotBlank(password)) {
            metadataBuilder.put("password", password);
        }
        if (StringUtils.isNotBlank(configPath)) {
            metadataBuilder.put("configPath", configPath);
        }
        return metadataBuilder.build();
    }

    private boolean isSchemeSecure(String scheme) {
        if (!WELL_KNOWN_URI_SCHEMES.containsKey(scheme)) {
            LOG.error("uri scheme {} is not supported, must be in {}", scheme, WELL_KNOWN_URI_SCHEMES.keySet());
            throw new IllegalArgumentException("unsupported config service URI scheme");
        }
        return HTTPS_SCHEME.equals(scheme);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceId() {
        return configServiceId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHost() {
        return host;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure() {
        return secure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getUri() {
        return baseUri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VaultBasedConfigServiceInstance)) {
            return false;
        }
        VaultBasedConfigServiceInstance that = (VaultBasedConfigServiceInstance) o;
        return getPort() == that.getPort() &&
                isSecure() == that.isSecure() &&
                Objects.equals(configServiceId, that.configServiceId) &&
                Objects.equals(getHost(), that.getHost()) &&
                Objects.equals(baseUri, that.baseUri) &&
                Objects.equals(getMetadata(), that.getMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(configServiceId, getHost(), getPort(), baseUri, isSecure(), getMetadata());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("configServiceId", configServiceId)
                .add("host", host)
                .add("port", port)
                .add("baseUri", baseUri)
                .add("secure", secure)
                .add("metadata", metadata)
                .toString();
    }
}
