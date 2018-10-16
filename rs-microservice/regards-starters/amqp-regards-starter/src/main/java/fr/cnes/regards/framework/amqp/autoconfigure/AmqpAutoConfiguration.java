/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import javax.annotation.PostConstruct;

import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

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
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.MultitenantRabbitTransactionManager;
import fr.cnes.regards.framework.amqp.configuration.MultitenantSimpleRoutingConnectionFactory;
import fr.cnes.regards.framework.amqp.configuration.RabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.VirtualHostMode;
import fr.cnes.regards.framework.amqp.converter.Gson2JsonMessageConverter;
import fr.cnes.regards.framework.amqp.single.SingleVhostPoller;
import fr.cnes.regards.framework.amqp.single.SingleVhostPublisher;
import fr.cnes.regards.framework.amqp.single.SingleVhostSubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantBootstrapProperties;

/**
 *
 * @author svissier
 *
 */
@Configuration
@ConditionalOnProperty(prefix = "regards.amqp", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({ RabbitProperties.class, AmqpManagementProperties.class,
        AmqpMicroserviceProperties.class })
@AutoConfigureAfter(name = { "fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration" })
@EnableTransactionManagement
public class AmqpAutoConfiguration {

    private static final String JACKSON_JSON_RABBIT_TEMPLATE_BEAN = "JacksonRabbitTemplate";

    private static final String GSON_RABBIT_TEMPLATE_BEAN = "GsonRabbitTemplate";

    @Autowired
    private MultitenantBootstrapProperties bootstrapProperties;

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

    /**
     * Initilization method
     */
    @PostConstruct
    public void init() {
        amqpProperties = new AmqpProperties(rabbitProperties, amqpManagmentProperties, amqpMicroserviceProperties);
    }

    @Bean
    @ConditionalOnMissingBean(IRabbitVirtualHostAdmin.class)
    public IRabbitVirtualHostAdmin rabbitVirtualHostAdmin(ITenantResolver pTenantResolver,
            final MultitenantSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory,
            final RestOperations restOperations) {
        return new RabbitVirtualHostAdmin(amqpManagmentProperties.getMode(), pTenantResolver,
                amqpProperties.getRabbitmqUserName(), amqpProperties.getRabbitmqPassword(),
                amqpProperties.getAmqpManagementHost(), amqpProperties.getAmqpManagementPort(), restOperations,
                pSimpleRoutingConnectionFactory, amqpProperties.getRabbitmqAddresses(),
                bootstrapProperties.getBootstrapTenants());
    }

    /**
     * @return a {@link RestTemplate} instance
     */
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public IAmqpAdmin regardsAmqpAdmin() {
        return new RegardsAmqpAdmin(amqpProperties.getTypeIdentifier(), amqpProperties.getInstanceIdentifier());
    }

    @Bean
    public RabbitAdmin rabbitAdmin(final SimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory) {
        return new RabbitAdmin(pSimpleRoutingConnectionFactory);
    }

    @Bean(name = JACKSON_JSON_RABBIT_TEMPLATE_BEAN)
    public RabbitTemplate jacksonTransactionalRabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(simpleRoutingConnectionFactory());
        // Enable transaction management : if action is executed in a transaction and transaction fails,
        // message is return to the broker.
        rabbitTemplate.setChannelTransacted(true);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * @return a {@link Jackson2JsonMessageConverter} instance used to (de)serialize events
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnBean(Gson.class)
    public Gson2JsonMessageConverter gson2JsonMessageConverter(Gson gson) {
        return new Gson2JsonMessageConverter(gson);
    }

    @Bean(name = GSON_RABBIT_TEMPLATE_BEAN)
    @ConditionalOnBean(Gson.class)
    public RabbitTemplate gsonTransactionalRabbitTemplate(Gson2JsonMessageConverter gsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(simpleRoutingConnectionFactory());
        // Enable transaction management : if action is executed in a transaction and transaction fails,
        // message is return to the broker.
        rabbitTemplate.setChannelTransacted(true);
        rabbitTemplate.setMessageConverter(gsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public IPublisher publisher(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver,
            @Autowired(required = false) @Qualifier(GSON_RABBIT_TEMPLATE_BEAN) RabbitTemplate gsonRabbitTemplate) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Publisher(pRabbitVirtualHostAdmin, jacksonTransactionalRabbitTemplate(), gsonRabbitTemplate,
                    amqpAdmin, pThreadTenantResolver);
        } else {
            return new SingleVhostPublisher(jacksonTransactionalRabbitTemplate(), gsonRabbitTemplate, amqpAdmin,
                    pRabbitVirtualHostAdmin, pThreadTenantResolver);
        }
    }

    @Bean
    public ISubscriber subscriber(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            @Autowired(required = false) Gson2JsonMessageConverter gson2JsonMessageConverter,
            ITenantResolver pTenantResolver) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Subscriber(pRabbitVirtualHostAdmin, amqpAdmin, jackson2JsonMessageConverter,
                    gson2JsonMessageConverter, pTenantResolver);
        } else {
            return new SingleVhostSubscriber(pRabbitVirtualHostAdmin, amqpAdmin, jackson2JsonMessageConverter,
                    gson2JsonMessageConverter, pTenantResolver);
        }
    }

    @Bean
    public IPoller poller(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver,
            @Autowired(required = false) @Qualifier(GSON_RABBIT_TEMPLATE_BEAN) RabbitTemplate gsonRabbitTemplate) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Poller(pRabbitVirtualHostAdmin, jacksonTransactionalRabbitTemplate(), gsonRabbitTemplate,
                    amqpAdmin, pThreadTenantResolver);
        } else {
            return new SingleVhostPoller(pRabbitVirtualHostAdmin, jacksonTransactionalRabbitTemplate(),
                    gsonRabbitTemplate, amqpAdmin, pThreadTenantResolver);
        }
    }

    @Bean
    public IInstancePublisher instancePublisher(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver,
            @Autowired(required = false) @Qualifier(GSON_RABBIT_TEMPLATE_BEAN) RabbitTemplate gsonRabbitTemplate) {
        return new InstancePublisher(jacksonTransactionalRabbitTemplate(), gsonRabbitTemplate, amqpAdmin,
                pRabbitVirtualHostAdmin);
    }

    @Bean
    public IInstanceSubscriber instanceSubscriber(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            Jackson2JsonMessageConverter jackson2JsonMessageConverter,
            @Autowired(required = false) Gson2JsonMessageConverter gson2JsonMessageConverter,
            ITenantResolver pTenantResolver) {
        return new InstanceSubscriber(pRabbitVirtualHostAdmin, amqpAdmin, jackson2JsonMessageConverter,
                gson2JsonMessageConverter);
    }

    @Bean
    public IInstancePoller instancePoller(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver,
            @Autowired(required = false) @Qualifier(GSON_RABBIT_TEMPLATE_BEAN) RabbitTemplate gsonRabbitTemplate) {
        return new InstancePoller(pRabbitVirtualHostAdmin, jacksonTransactionalRabbitTemplate(), gsonRabbitTemplate,
                amqpAdmin);
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
        return new MultitenantRabbitTransactionManager(amqpManagmentProperties.getMode(),
                simpleRoutingConnectionFactory(), pThreadTenantResolver, pRabbitVirtualHostAdmin);
    }

    @Bean
    public AmqpEventHandler amqpEventHandler(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            IInstanceSubscriber pInstanceSubscriber, ISubscriber pSubscriber) {
        return new AmqpEventHandler(pRabbitVirtualHostAdmin, pInstanceSubscriber, pSubscriber);
    }
}
