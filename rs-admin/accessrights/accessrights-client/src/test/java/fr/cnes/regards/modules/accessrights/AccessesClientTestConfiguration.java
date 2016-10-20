package fr.cnes.regards.modules.accessrights;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.client.core.ClientRequestInterceptor;

@Configuration
@ComponentScan
@EnableFeignClients(defaultConfiguration = { ClientRequestInterceptor.class })
@EnableAutoConfiguration
public class AccessesClientTestConfiguration {

}
