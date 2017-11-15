package com.haufe.demo.spring.configserver;

import com.haufe.demo.spring.configserver.security.ConfigServerSecurityProperties;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests that the config server enforces the submission of valid HTTP basic auth credentials if both
 * configuration properties {@code spring.cloud.config.username} and {@code spring.cloud.config.password} are set.
 * <p>
 * This test does <em>not</em> cover fetching these configuration properties from Vault!
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles({"integrationtest"})
@TestPropertySource(properties = {
        "haufe.configserver.security.basicAuth.username=idefix",
        "haufe.configserver.security.basicAuth.password=mistel"
})
public class BasicAuthConfigServerApplicationTest {

    // The Spring Boot Test framework ensures the testRestTemplate uses the correct endpoint for the service
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ConfigServerSecurityProperties configServerSecurityProperties;

	@Test
    public void testThatTestserviceConfigIsReturnedIfCorrectCredentialsAreProvided() {
        ConfigServerSecurityProperties.BasicAuthProperties basicAuth = configServerSecurityProperties.getBasicAuth();
        injectBasicAuthClientHttpRequestInterceptor(basicAuth.getUsername(), basicAuth.getPassword());

        ResponseEntity<String> entity = this.testRestTemplate.getForEntity("/testservice/default/", String.class);
        assertThat(entity.getStatusCodeValue()).isEqualTo(200);
        assertThat(entity.getBody()).isNotEmpty();
        DocumentContext context = JsonPath.parse(entity.getBody());
        assertThat(context.<String>read("$.name")).isEqualTo("testservice");
        assertThat(context.<List>read("$.profiles")).isEqualTo(Collections.singletonList("default"));
        assertThat(context.<List>read("$.propertySources[?(@.name =~ /.*testservice.yml/)].source['foo.bar']"))
                .isEqualTo(Collections.singletonList("baz"));
    }

    @Test
    public void testThatAccessIsDeniedIfIncorrectCredentialsAreProvided() {
        ConfigServerSecurityProperties.BasicAuthProperties basicAuth = configServerSecurityProperties.getBasicAuth();
        injectBasicAuthClientHttpRequestInterceptor(basicAuth.getUsername(), basicAuth.getPassword() + "xyz");

        ResponseEntity<String> entity = this.testRestTemplate.getForEntity("/testservice/default/", String.class);
        assertThat(entity.getStatusCodeValue()).isEqualTo(401);
    }

    @Test
    public void testThatAccessIsDeniedIfNoCredentialsAreProvided() {
        ConfigServerSecurityProperties.BasicAuthProperties basicAuth = configServerSecurityProperties.getBasicAuth();
        injectBasicAuthClientHttpRequestInterceptor(basicAuth.getUsername(), basicAuth.getPassword() + "xyz");

        ResponseEntity<String> entity = this.testRestTemplate.getForEntity("/testservice/default/", String.class);
        assertThat(entity.getStatusCodeValue()).isEqualTo(401);
    }

    private void injectBasicAuthClientHttpRequestInterceptor(String username, String password) {
        List<ClientHttpRequestInterceptor> interceptors =
                StringUtils.isNoneBlank(username, password) ?
                Collections.singletonList(new BasicAuthorizationInterceptor(username, password)) :
                Collections.emptyList();
        testRestTemplate.getRestTemplate().setInterceptors(interceptors);
    }

    private static class BasicAuthorizationInterceptor implements
            ClientHttpRequestInterceptor {

        private final String username;

        private final String password;

        BasicAuthorizationInterceptor(String username, String password) {
            this.username = username;
            this.password = (password == null ? "" : password);
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            byte[] token = Base64Utils.encode((this.username + ":" + this.password).getBytes());
            request.getHeaders().add("Authorization", "Basic " + new String(token));
            return execution.execute(request, body);
        }

    }
}
