package com.haufe.demo.spring.configserver.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Type-safe configuration object for the config server's security setup.
 */
@ConfigurationProperties("haufe.configserver.security")
public class ConfigServerSecurityProperties {

    /**
     * The enclosed HTTP basic auth configuration properties.
     */
    private BasicAuthProperties basicAuth = new BasicAuthProperties();

    /**
     * The enclosed basic auth properties.
     *
     * @return basic auth properties object, never {@literal null}
     */
    public BasicAuthProperties getBasicAuth() {
        return basicAuth;
    }

    /**
     * Overwrite the basic auth properties
     *
     * @param basicAuthProperties
     *         the new basic auth properties
     */
    public void setBasicAuth(BasicAuthProperties basicAuthProperties) {
        this.basicAuth = basicAuthProperties != null ? basicAuthProperties : new BasicAuthProperties();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfigServerSecurityProperties)) {
            return false;
        }
        ConfigServerSecurityProperties that = (ConfigServerSecurityProperties) o;
        return Objects.equals(getBasicAuth(), that.getBasicAuth());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBasicAuth());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigServerSecurityProperties{");
        sb.append("basicAuth=").append(basicAuth);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Type-safe configuration object for the config server's basic auth setup.
     */
    public static class BasicAuthProperties {

        /**
         * The basic auth username required to access the config server.
         * Basic authentication will be disabled if the username is blank.
         */
        private String username;

        /**
         * The basic auth password required to access the config server.
         * Basic authentication will be disabled if the username is blank.
         */
        private String password;

        /**
         * The basic auth username required to access the config server.
         *
         * @return the username, might be {@literal null} or empty
         */
        public String getUsername() {
            return username;
        }

        /**
         * Set the basic auth username.
         *
         * @param username the new username, might be {@literal null} or empty
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * The basic auth password required to access the config server.
         *
         * @return the password, might be {@literal null} or empty
         */
        public String getPassword() {
            return password;
        }

        /**
         * Set the basic auth password.
         *
         * @param password the new username, might be {@literal null} or empty
         */
        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BasicAuthProperties)) {
                return false;
            }
            BasicAuthProperties that = (BasicAuthProperties) o;
            return Objects.equals(getUsername(), that.getUsername()) &&
                    Objects.equals(getPassword(), that.getPassword());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getUsername(), getPassword());
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("BasicAuthProperties{");
            sb.append("username='").append(username).append('\'');
            sb.append(", password='").append(password == null ? "<null>" : "***").append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
