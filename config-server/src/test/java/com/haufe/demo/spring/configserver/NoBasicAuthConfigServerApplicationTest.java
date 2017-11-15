package com.haufe.demo.spring.configserver;

import com.haufe.demo.spring.configserver.security.ConfigServerSecurityProperties;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test that the config service is available without credentials if the the configuration properties
 * {@code haufe.configserver.security.basicauth.username} and {@code haufe.configserver.security.basicauth.password}
 * are <em>not</em> set.
 *
 * @see com.haufe.demo.spring.configserver.security.WebSecurityConfig#isBasicAuthEnabled(ConfigServerSecurityProperties.BasicAuthProperties)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles({"integrationtest"})
@TestPropertySource(properties = {
        "haufe.configserver.security.basicAuth.username=",
        "haufe.configserver.security.basicAuth.password="
})
public class NoBasicAuthConfigServerApplicationTest {

    // The Spring Boot Test framework ensures the restTemplate uses the correct endpoint for the service
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testThatTestserviceConfigIsReturned() {
        ResponseEntity<String> entity = this.restTemplate.getForEntity("/testservice/default/", String.class);
        assertThat(entity.getStatusCodeValue()).isEqualTo(200);
        assertThat(entity.getBody()).isNotEmpty();
        DocumentContext context = JsonPath.parse(entity.getBody());
        assertThat(context.<String>read("$.name")).isEqualTo("testservice");
        assertThat(context.<List>read("$.profiles")).isEqualTo(Collections.singletonList("default"));
        assertThat(context.<List>read("$.propertySources[?(@.name =~ /.*testservice.yml/)].source['foo.bar']"))
                .isEqualTo(Collections.singletonList("baz"));
    }
}
