package com.haufe.demo.spring.vaultconfigdemo;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC Controller for the Vault Config Demo application.
 */
@RestController
@RequestMapping("/")
public class VaultConfigDemoController {

    private final Environment environment;

    public VaultConfigDemoController(Environment environment) {
        this.environment = environment;
    }

    /**
     * An endpoint that returns the active profiles as well as the value of the specified property.
     * <p>
     * <b>Note:</b> This is a demo application only, so no validation of the input parameter is done.
     * Use at your own risk.
     *
     * @param name
     *         the name of the requested configuration property, must not be {@code null}
     * @return a response entity that holds a map with the list of active spring profiles
     * and the requested configuration parameter (if found); never {@code null}
     */
    @GetMapping(path = "/property/{name}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> displayEnvironment(@PathVariable String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));

        String property = environment.getProperty(name);
        if (property == null) {
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        }
        result.put(name, property);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
