package fr.cnes.regards.modules.processing.conf;

import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;
import reactivefeign.spring.config.EnableReactiveFeignClients;

@EnableEurekaClient
@EnableReactiveFeignClients
@Configuration
public class FeignWebclientConfig {

}
