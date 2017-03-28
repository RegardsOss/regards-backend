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
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import fr.cnes.regards.framework.amqp.IInstancePoller;
import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.InstancePoller;
import fr.cnes.regards.framework.amqp.InstancePublisher;
import fr.cnes.regards.framework.amqp.InstanceSubscriber;
import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.MultitenantRabbitTransactionManager;
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
@EnableConfigurationProperties({ RabbitProperties.class, AmqpManagementProperties.class,
        AmqpMicroserviceProperties.class })
@EnableTransactionManagement
public class AmqpAutoConfiguration {

    /**
     * bean providing properties from the configuration file
     */
    @Autowired
    private RabbitProperties rabbitProperties;

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
        amqpProperties = new AmqpProperties(rabbitProperties, amqpManagmentProperties, amqpMicroserviceProperties);
    }

    @Bean
    @ConditionalOnMissingBean(IRabbitVirtualHostAdmin.class)
    public IRabbitVirtualHostAdmin rabbitVirtualHostAdmin(ITenantResolver pTenantResolver,
            final MultitenantSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory,
            final RestTemplate pRestTemplate) {
        return new RabbitVirtualHostAdmin(pTenantResolver, amqpProperties.getRabbitmqUserName(),
                amqpProperties.getRabbitmqPassword(), amqpProperties.getAmqpManagementHost(),
                amqpProperties.getAmqpManagementPort(), pRestTemplate, pSimpleRoutingConnectionFactory,
                amqpProperties.getRabbitmqAddresses());
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
    public RabbitTemplate transactionalRabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(simpleRoutingConnectionFactory());
        // Enable transaction management : if action is executed in a transaction and transaction fails,
        // message is return to the broker.
        rabbitTemplate.setChannelTransacted(true);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public IPublisher publisher(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver) {
        return new Publisher(pRabbitVirtualHostAdmin, transactionalRabbitTemplate(), pRegardsAmqpAdmin,
                pThreadTenantResolver);
    }

    @Bean
    public ISubscriber subscriber(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            final RegardsAmqpAdmin pRegardsAmqpAdmin, final Jackson2JsonMessageConverter pJackson2JsonMessageConverter,
            final ITenantResolver pTenantResolver) {
        return new Subscriber(pRabbitVirtualHostAdmin, pRegardsAmqpAdmin, pJackson2JsonMessageConverter,
                pTenantResolver);
    }

    @Bean
    public IPoller poller(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver) {
        return new Poller(pRabbitVirtualHostAdmin, transactionalRabbitTemplate(), pRegardsAmqpAdmin,
                pThreadTenantResolver);
    }

    @Bean
    public IInstancePublisher instancePublisher(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            RegardsAmqpAdmin pRegardsAmqpAdmin, IRuntimeTenantResolver pThreadTenantResolver) {
        return new InstancePublisher(transactionalRabbitTemplate(), pRegardsAmqpAdmin, pRabbitVirtualHostAdmin);
    }

    @Bean
    public IInstanceSubscriber instanceSubscriber(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            final RegardsAmqpAdmin pRegardsAmqpAdmin, final Jackson2JsonMessageConverter pJackson2JsonMessageConverter,
            final ITenantResolver pTenantResolver) {
        return new InstanceSubscriber(pRabbitVirtualHostAdmin, pRegardsAmqpAdmin, pJackson2JsonMessageConverter);
    }

    @Bean
    public IInstancePoller instancePoller(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            RegardsAmqpAdmin pRegardsAmqpAdmin, IRuntimeTenantResolver pThreadTenantResolver) {
        return new InstancePoller(pRabbitVirtualHostAdmin, transactionalRabbitTemplate(), pRegardsAmqpAdmin);
    }

    @Bean
    public MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory() {
        return new MultitenantSimpleRoutingConnectionFactory();
    }

    /**
     * This bean is only useful if no {@link PlatformTransactionManager} was provided by a database or else.
     *
     * @param pThreadTenantResolver
     *            runtime tenant resolver
     * @param pRabbitVirtualHostAdmin
     *            virtual host admin
     * @return a {@link RabbitTransactionManager}
     */
    @Bean
    @ConditionalOnProperty(prefix = "regards.amqp", name = "internal.transaction", matchIfMissing = false)
    public PlatformTransactionManager rabbitTransactionManager(IRuntimeTenantResolver pThreadTenantResolver,
            IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        return new MultitenantRabbitTransactionManager(simpleRoutingConnectionFactory(), pThreadTenantResolver,
                pRabbitVirtualHostAdmin);
    }

    @Bean
    public AmqpEventHandler amqpEventHandler(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            IInstanceSubscriber pSubscriber) {
        return new AmqpEventHandler(pRabbitVirtualHostAdmin, pSubscriber);
    }
}
