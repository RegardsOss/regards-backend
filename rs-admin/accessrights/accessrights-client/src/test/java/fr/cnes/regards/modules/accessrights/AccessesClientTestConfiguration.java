package fr.cnes.regards.modules.accessrights;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.client.core.ClientRequestInterceptor;

@Configuration
@ComponentScan("fr.cnes.regards.modules")
@EnableFeignClients(defaultConfiguration = { ClientRequestInterceptor.class })
@EnableAutoConfiguration
@PropertySource("classpath:test.properties")
public class AccessesClientTestConfiguration {

}
