package com.haufe.demo.spring.configserver.security;

import com.haufe.demo.spring.configserver.security.ConfigServerSecurityProperties.BasicAuthProperties;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Unit tests of {@link WebSecurityConfig}.
 */
public class WebSecurityConfigTest {

    @Test
    public void testThatBasicAuthIsEnabledIfUsernameAndPasswordAreSet() {
        WebSecurityConfig webSecurityConfig = createWebSecurityConfig("user", "pw");

        assertThat(webSecurityConfig.isBasicAuthEnabled(), is(true));
    }

    @Test
    public void testThatBasicAuthIsDisabledIfUsernameIsNull() {
        WebSecurityConfig webSecurityConfig = createWebSecurityConfig(null, "pw");
        assertThat(webSecurityConfig.isBasicAuthEnabled(), is(false));
    }

    @Test
    public void testThatBasicAuthIsDisabledIfUsernameIsEmpty() {
        WebSecurityConfig webSecurityConfig = createWebSecurityConfig("", "pw");
        assertThat(webSecurityConfig.isBasicAuthEnabled(), is(false));
    }

    @Test
    public void testThatBasicAuthIsDisabledIfUsernameIsBlank() {
        WebSecurityConfig webSecurityConfig = createWebSecurityConfig(" \n\t", "pw");
        assertThat(webSecurityConfig.isBasicAuthEnabled(), is(false));
    }

    @Test
    public void testThatBasicAuthIsDisabledIfPasswordisNull() {
        WebSecurityConfig webSecurityConfig = createWebSecurityConfig("user", null);

        assertThat(webSecurityConfig.isBasicAuthEnabled(), is(false));
    }

    @Test
    public void testThatBasicAuthIsDisabledIfPasswordisEmpty() {
        WebSecurityConfig webSecurityConfig = createWebSecurityConfig("user", "");

        assertThat(webSecurityConfig.isBasicAuthEnabled(), is(false));
    }

    @Test
    public void testThatBasicAuthIsDisabledIfPasswordisBlank() {
        WebSecurityConfig webSecurityConfig = createWebSecurityConfig("user", " \n\t");

        assertThat(webSecurityConfig.isBasicAuthEnabled(), is(false));
    }

    private WebSecurityConfig createWebSecurityConfig(String username, String password) {
        BasicAuthProperties basicAuthProperties = new BasicAuthProperties();
        basicAuthProperties.setUsername(username);
        basicAuthProperties.setPassword(password);
        ConfigServerSecurityProperties serverSecurityProperties = new ConfigServerSecurityProperties();
        serverSecurityProperties.setBasicAuth(basicAuthProperties);

        return new WebSecurityConfig(serverSecurityProperties);
    }

}