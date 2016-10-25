/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import javax.annotation.PostConstruct;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.connection.RegardsSimpleRoutingConnectionFactory;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;

/**
 *
 * @author svissier
 *
 */
@Configuration
@AutoConfigureAfter(MultitenantAutoConfiguration.class)
@ConditionalOnProperty(prefix = "regards.amqp", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({ SpringRabbitMQProperties.class, AmqpManagementProperties.class,
        AmqpMicroserviceProperties.class })
public class AmqpAutoConfiguration {

    /**
     * bean providing properties from the configuration file
     */
    @Autowired
    private SpringRabbitMQProperties springRabbitMQProperties;

    /**
     * bean providing properties from the configuration file
     */
    @Autowired
    private AmqpManagementProperties amqpManagmentProperties;

    /**
     * bean providing properties from the configuration file
     */
    @Autowired
    private AmqpMicroserviceProperties amqpMicroserviceProperties;

    /**
     * class regrouping accesses to all properties used by the client
     */
    private AmqpProperties amqpProperties;

    @PostConstruct
    public void init() {
        amqpProperties = new AmqpProperties(springRabbitMQProperties, amqpManagmentProperties,
                amqpMicroserviceProperties);
    }

    @Bean
    @ConditionalOnMissingBean(IRabbitVirtualHostUtils.class)
    public IRabbitVirtualHostUtils iRabbitVirtualHostUtils(
            RegardsSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory, RestTemplate pRestTemplate) {
        return new RabbitVirtualHostUtils(amqpProperties.getRabbitmqUserName(), amqpProperties.getRabbitmqPassword(),
                amqpProperties.getAmqpManagementHost(), amqpProperties.getAmqpManagementPort(), pRestTemplate,
                pSimpleRoutingConnectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RegardsAmqpAdmin regardsAmqpAdmin() {
        return new RegardsAmqpAdmin(amqpProperties.getTypeIdentifier(), amqpProperties.getInstanceIdentifier(),
                amqpProperties.getRabbitmqAddresses());
    }

    @Bean
    public RabbitAdmin rabbitAdmin(RegardsSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory) {
        return new RabbitAdmin(pSimpleRoutingConnectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(RegardsSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(pSimpleRoutingConnectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Publisher publisher(IRabbitVirtualHostUtils pIRabbitVirtualHostUtils, RabbitTemplate pRabbitTemplate,
            RegardsAmqpAdmin pRegardsAmqpAdmin) {
        return new Publisher(pRabbitTemplate, pRegardsAmqpAdmin, pIRabbitVirtualHostUtils);
    }

    @Bean
    public Subscriber subscriber(RegardsAmqpAdmin pRegardsAmqpAdmin, IRabbitVirtualHostUtils pIRabbitVirtualHostUtils,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter, ITenantResolver pTenantResolver) {
        return new Subscriber(pRegardsAmqpAdmin, pIRabbitVirtualHostUtils, pJackson2JsonMessageConverter,
                pTenantResolver);
    }

    @Bean
    public Poller poller(RabbitTemplate pRabbitTemplate, RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRabbitVirtualHostUtils pIRabbitVirtualHostUtils) {
        return new Poller(pRabbitTemplate, pRegardsAmqpAdmin, pIRabbitVirtualHostUtils);
    }

    @Bean
    public RegardsSimpleRoutingConnectionFactory regardsSimpleRoutingConnectionFactory() {
        return new RegardsSimpleRoutingConnectionFactory();
    }
}
