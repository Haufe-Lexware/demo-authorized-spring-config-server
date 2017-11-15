package com.haufe.demo.spring.vaultconfigdemo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles({"integrationtest", "no-config-server"})
@SpringBootTest
public class DisabledConfigClientApplicationTests {

	@Test
	public void contextLoads() {
	}

}
