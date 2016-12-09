/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import javax.annotation.PostConstruct;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.connection.RegardsSimpleRoutingConnectionFactory;
import fr.cnes.regards.framework.amqp.utils.IRabbitVirtualHostUtils;
import fr.cnes.regards.framework.amqp.utils.RabbitVirtualHostUtils;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;

/**
 *
 * @author svissier
 *
 */
@Configuration
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
            final RegardsSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory,
            final RestTemplate pRestTemplate, final RegardsAmqpAdmin pRegardsAmqpAdmin) {
        return new RabbitVirtualHostUtils(amqpProperties.getRabbitmqUserName(), amqpProperties.getRabbitmqPassword(),
                amqpProperties.getAmqpManagementHost(), amqpProperties.getAmqpManagementPort(), pRestTemplate,
                pSimpleRoutingConnectionFactory, pRegardsAmqpAdmin);
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
    public RabbitAdmin rabbitAdmin(final RegardsSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory) {
        return new RabbitAdmin(pSimpleRoutingConnectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final RegardsSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory,
            final Jackson2JsonMessageConverter pJackson2JsonMessageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(pSimpleRoutingConnectionFactory);
        rabbitTemplate.setMessageConverter(pJackson2JsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public IPublisher publisher(final IRabbitVirtualHostUtils pIRabbitVirtualHostUtils,
            final RabbitTemplate pRabbitTemplate, final RegardsAmqpAdmin pRegardsAmqpAdmin) {
        return new Publisher(pRabbitTemplate, pRegardsAmqpAdmin, pIRabbitVirtualHostUtils);
    }

    @Bean
    public Subscriber subscriber(final RegardsAmqpAdmin pRegardsAmqpAdmin,
            final IRabbitVirtualHostUtils pIRabbitVirtualHostUtils,
            final Jackson2JsonMessageConverter pJackson2JsonMessageConverter, final ITenantResolver pTenantResolver) {
        return new Subscriber(pRegardsAmqpAdmin, pIRabbitVirtualHostUtils, pJackson2JsonMessageConverter,
                pTenantResolver);
    }

    @Bean
    public Poller poller(final RabbitTemplate pRabbitTemplate, final RegardsAmqpAdmin pRegardsAmqpAdmin,
            final IRabbitVirtualHostUtils pIRabbitVirtualHostUtils) {
        return new Poller(pRabbitTemplate, pRegardsAmqpAdmin, pIRabbitVirtualHostUtils);
    }

    @Bean
    public RegardsSimpleRoutingConnectionFactory regardsSimpleRoutingConnectionFactory() {
        return new RegardsSimpleRoutingConnectionFactory();
    }
}
