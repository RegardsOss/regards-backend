package fr.cnes.regards.modules.accessrights.client;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;

@Configuration
@EnableFeignClients(clients = IRegistrationClient.class)
@EnableAutoConfiguration
@PropertySource("classpath:test.properties")
public class RegistrationClientTestConfiguration {

    @Bean
    ISubscriber eventSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    @Bean
    IPublisher eventPublisher() {
        return Mockito.mock(IPublisher.class);
    }

}
