package fr.cnes.regards.modules.accessrights.client;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableFeignClients(clients = IRegistrationClient.class)
@EnableAutoConfiguration
@PropertySource("classpath:test.properties")
public class RegistrationClientTestConfiguration {

}
