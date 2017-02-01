/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import javax.annotation.PostConstruct;

import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.MultitenantSimpleRoutingConnectionFactory;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 *
 * @author svissier
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "regards.amqp", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({ SpringRabbitMQProperties.class, AmqpManagementProperties.class,
        AmqpMicroserviceProperties.class })
@EnableTransactionManagement
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
    @ConditionalOnMissingBean(IRabbitVirtualHostAdmin.class)
    public IRabbitVirtualHostAdmin rabbitVirtualHostAdmin(
            final MultitenantSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory,
            final RestTemplate pRestTemplate) {
        return new RabbitVirtualHostAdmin(amqpProperties.getRabbitmqUserName(), amqpProperties.getRabbitmqPassword(),
                amqpProperties.getAmqpManagementHost(), amqpProperties.getAmqpManagementPort(), pRestTemplate,
                pSimpleRoutingConnectionFactory, amqpProperties.getRabbitmqAddresses());
    }

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RegardsAmqpAdmin regardsAmqpAdmin() {
        return new RegardsAmqpAdmin(amqpProperties.getTypeIdentifier(), amqpProperties.getInstanceIdentifier());
    }

    @Bean
    public RabbitAdmin rabbitAdmin(final SimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory) {
        return new RabbitAdmin(pSimpleRoutingConnectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final SimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory,
            final Jackson2JsonMessageConverter pJackson2JsonMessageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(pSimpleRoutingConnectionFactory);
        // Enable transaction management : if action is executed in a transaction and transaction fails,
        // message is return to the broker.
        rabbitTemplate.setChannelTransacted(true);
        rabbitTemplate.setMessageConverter(pJackson2JsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public IPublisher publisher(final IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            final RabbitTemplate pRabbitTemplate, final RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver) {
        return new Publisher(pRabbitTemplate, pRegardsAmqpAdmin, pRabbitVirtualHostAdmin, pThreadTenantResolver);
    }

    @Bean
    public Subscriber subscriber(final RegardsAmqpAdmin pRegardsAmqpAdmin,
            final IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            final Jackson2JsonMessageConverter pJackson2JsonMessageConverter, final ITenantResolver pTenantResolver) {
        return new Subscriber(pRegardsAmqpAdmin, pRabbitVirtualHostAdmin, pJackson2JsonMessageConverter,
                pTenantResolver);
    }

    @Bean
    public IPoller poller(final RabbitTemplate pRabbitTemplate, final RegardsAmqpAdmin pRegardsAmqpAdmin,
            final IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        return new Poller(pRabbitTemplate, pRegardsAmqpAdmin, pRabbitVirtualHostAdmin);
    }

    @Bean
    public MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory() {
        return new MultitenantSimpleRoutingConnectionFactory();
    }

    @Bean
    public RabbitTransactionManager transactionManager() {
        return new RabbitTransactionManager(simpleRoutingConnectionFactory());
    }
}
