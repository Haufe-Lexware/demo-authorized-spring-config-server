package com.haufe.demo.spring.configserver.security;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Spring Security Java Configuration for the Haufe Config Server, active only
 * if the configuration properties {@code haufe.configserver.security.basicAuth.username} and
 * {@code haufe.configserver.security.basicAuth.password} are
 * {@link org.apache.commons.lang3.StringUtils#isNotBlank(CharSequence) not blank}.
 * <p>
 * <b>Remark:</b> Cloud Config clients expect the basic auth credentials in the configuration properties
 * {@code spring.cloud.config.username} and {@code spring.cloud.config.password}, respectively. If the config server
 * is suppoosed to read the credentials from Vault as well, then one can map {@code spring.cloud.config.username} to
 * {@code haufe.configserver.security.basicAuth.username} in the properties configuration
 * (and similarly for the password).
 *
 * @see ConfigServerSecurityProperties
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ConfigServerSecurityProperties.class)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

    private static final SimpleGrantedAuthority USER_GRANTED_AUTHORITY = new SimpleGrantedAuthority("ROLE_USER");

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ConfigServerSecurityProperties configServerSecurityProperties;
    private final boolean basicAuthEnabled;


    /**
     * Constructor that injects the config client credentials.
     * <p>
     * We kind of abuse the basic authentication for access control - only clients that present the credentials
     * will be able to fetch configurations. However, we do not really care about the technical id of the client
     * (i.e., process id on a specific host, possibly behind a NAT). We simply want to make sure the config client
     * is not open to anybody on the internet.
     * <p>
     * In this case, there is no point in providing the password in hashed form - the client would have to send
     * it in (base64 encoded) plain text anyway, so the non-hashed password has to be stored in the vault server.
     *
     * @param configServerSecurityProperties
     *         the configuration server's security configuration properties,
     *         must not be {@literal null}
     */
    @Autowired
    public WebSecurityConfig(ConfigServerSecurityProperties configServerSecurityProperties) {
        Objects.requireNonNull(configServerSecurityProperties,
                               "configServerSecurityProperties must not be null");
        this.configServerSecurityProperties = configServerSecurityProperties;
        this.basicAuthEnabled = isBasicAuthEnabled(getBasicAuthProperties());
    }

    /**
     * Getter for property 'basicAuthEnabled'. This web security configuration adapter has an effect if and only if
     * basic auth <em>is</em> enabled.
     *
     * @return Value for property 'basicAuthEnabled'.
     */
    public boolean isBasicAuthEnabled() {
        return basicAuthEnabled;
    }

    private ConfigServerSecurityProperties.BasicAuthProperties getBasicAuthProperties() {
        ConfigServerSecurityProperties.BasicAuthProperties basicAuth = configServerSecurityProperties.getBasicAuth();
        assert basicAuth != null : "configServerSecurityProperties.getBasicAuth() must not return null";
        return basicAuth;
    }

    private static boolean isBasicAuthEnabled(ConfigServerSecurityProperties.BasicAuthProperties basicAuth) {
        String username = basicAuth.getUsername();
        String password = basicAuth.getPassword();

        boolean noneBlank = StringUtils.isNoneBlank(username, password);
        if (!noneBlank) {
            if (StringUtils.isBlank(username)) {
                LOG.warn("haufe.configserver.security.basicauth.username is blank, basic authentication disabled");
            }
            if (StringUtils.isBlank(password)) {
                LOG.warn("haufe.configserver.security.basicauth.password is blank, basic authentication disabled");
            }
        }
        else {
            LOG.info("username and password configured, no access at all without HTTP basic authentication");
        }
        return noneBlank;
    }

    /**
     * Make Spring Security require basic authentication on _any_ endpoint if both username and password are configured.
     *
     * @param httpSecurity the {@link HttpSecurity} object to configure, must not be {@literal null}
     * @throws Exception configuration of {@code httpSecurity} failed
     */
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        Objects.requireNonNull(httpSecurity, "httpSecurity must not be null");
        if (basicAuthEnabled) {
            httpSecurity
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic();
        }
    }

    /**
     * User detail serice backed by an {@link InMemoryUserDetailsManager in-memory store} that holds the credentials
     * found in the {@link ConfigServerSecurityProperties.BasicAuthProperties basic auth properties}.
     *
     * @return a user detail service with the config client credentials.
     */
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        Set<UserDetails> userDetailsCollection;
        if(basicAuthEnabled) {
            ConfigServerSecurityProperties.BasicAuthProperties basicAuth = getBasicAuthProperties();

            String passwordhash = passwordEncoder.encode(StringUtils.defaultString(basicAuth.getPassword()));
            User user = new User(basicAuth.getUsername(), passwordhash, Collections.singleton(USER_GRANTED_AUTHORITY));
            userDetailsCollection = Collections.singleton(user);
        }
        else {
            userDetailsCollection = Collections.emptySet();
        }
        return new InMemoryUserDetailsManager(userDetailsCollection);
    }

    /**
     * Make the {@link AuthenticationManagerBuilder} use the in-memory user detail service with hashed passwords.
     *
     * @param auth
     *         the bulder foer the authentication manager.
     * @throws Exception an error occured when adding the {@link UserDetailsService} based authentication
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService())
                .passwordEncoder(passwordEncoder);
    }

}
