/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
import fr.cnes.regards.framework.amqp.configuration.RegardsErrorHandler;
import fr.cnes.regards.framework.amqp.configuration.VirtualHostMode;
import fr.cnes.regards.framework.amqp.converter.Gson2JsonMessageConverter;
import fr.cnes.regards.framework.amqp.converter.JsonMessageConverters;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.single.SingleVhostPoller;
import fr.cnes.regards.framework.amqp.single.SingleVhostPublisher;
import fr.cnes.regards.framework.amqp.single.SingleVhostSubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantBootstrapProperties;

/**
 * @author svissier
 */
@Configuration
@ConditionalOnProperty(prefix = "regards.amqp", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({ RabbitProperties.class, AmqpManagementProperties.class,
        AmqpMicroserviceProperties.class })
@AutoConfigureAfter(name = { "fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration" })
@EnableTransactionManagement
public class AmqpAutoConfiguration {

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

    @Value("${spring.application.name}")
    private String microserviceName;

    @Bean
    @ConditionalOnMissingBean(IRabbitVirtualHostAdmin.class)
    public IRabbitVirtualHostAdmin rabbitVirtualHostAdmin(ITenantResolver pTenantResolver,
            final MultitenantSimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory,
            final RestOperations restOperations) {
        return new RabbitVirtualHostAdmin(amqpManagmentProperties.getMode(), pTenantResolver,
                rabbitProperties.getUsername(), rabbitProperties.getPassword(), amqpManagmentProperties.getHost(),
                amqpManagmentProperties.getPort(), restOperations, pSimpleRoutingConnectionFactory,
                rabbitProperties.determineAddresses(), bootstrapProperties.getBootstrapTenants());
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
        return new RegardsAmqpAdmin(amqpManagmentProperties.getNamespace(),
                amqpMicroserviceProperties.getTypeIdentifier());
    }

    @Bean
    public RegardsErrorHandler errorHandler(IRuntimeTenantResolver runtimeTenantResolver,
            IInstancePublisher instancePublisher, IPublisher publisher) {
        return new RegardsErrorHandler(runtimeTenantResolver, instancePublisher, publisher, microserviceName);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(final SimpleRoutingConnectionFactory pSimpleRoutingConnectionFactory) {
        return new RabbitAdmin(pSimpleRoutingConnectionFactory);
    }

    @Bean
    public RabbitTemplate transactionalRabbitTemplate(MessageConverter jsonMessageConverters) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(simpleRoutingConnectionFactory());
        // Enable transaction management : if action is executed in a transaction and transaction fails,
        // message is return to the broker.
        rabbitTemplate.setChannelTransacted(true);
        rabbitTemplate.setMessageConverter(jsonMessageConverters);
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter jsonMessageConverters(@Autowired(required = false) Gson gson,
            IRuntimeTenantResolver runtimeTenantResolver) {

        JsonMessageConverters converters = new JsonMessageConverters(runtimeTenantResolver);

        // Register Jackson
        Jackson2JsonMessageConverter jacksonConverter = new Jackson2JsonMessageConverter();
        jacksonConverter.setTypePrecedence(TypePrecedence.INFERRED);
        converters.registerConverter(JsonMessageConverter.JACKSON, jacksonConverter);

        // Register GSON
        if (gson != null) {
            Gson2JsonMessageConverter gsonConverter = new Gson2JsonMessageConverter(gson);
            converters.registerConverter(JsonMessageConverter.GSON, gsonConverter);
        }
        return converters;
    }

    @Bean
    public IPublisher publisher(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, RabbitAdmin rabbitAdmin,
            IAmqpAdmin amqpAdmin, IRuntimeTenantResolver pThreadTenantResolver, RabbitTemplate rabbitTemplate) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Publisher(pRabbitVirtualHostAdmin, rabbitTemplate, rabbitAdmin, amqpAdmin,
                    pThreadTenantResolver);
        } else {
            return new SingleVhostPublisher(rabbitTemplate, rabbitAdmin, amqpAdmin, pRabbitVirtualHostAdmin,
                    pThreadTenantResolver);
        }
    }

    @Bean
    public ISubscriber subscriber(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, RabbitAdmin rabbitAdmin,
            IAmqpAdmin amqpAdmin, MessageConverter jsonMessageConverters, ITenantResolver pTenantResolver,
            RegardsErrorHandler errorHandler, IRuntimeTenantResolver runtimeTenantResolver,
            IInstancePublisher instancePublisher, IPublisher publisher) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Subscriber(pRabbitVirtualHostAdmin, amqpAdmin, jsonMessageConverters, pTenantResolver,
                    errorHandler, microserviceName, instancePublisher, publisher, runtimeTenantResolver);
        } else {
            return new SingleVhostSubscriber(pRabbitVirtualHostAdmin, amqpAdmin, jsonMessageConverters, pTenantResolver,
                    errorHandler, microserviceName, instancePublisher, publisher, runtimeTenantResolver);
        }
    }

    @Bean
    public IPoller poller(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver, RabbitTemplate rabbitTemplate) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Poller(pRabbitVirtualHostAdmin, rabbitTemplate, amqpAdmin, pThreadTenantResolver);
        } else {
            return new SingleVhostPoller(pRabbitVirtualHostAdmin, rabbitTemplate, amqpAdmin, pThreadTenantResolver);
        }
    }

    @Bean
    public IInstancePublisher instancePublisher(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            RabbitAdmin rabbitAdmin, IAmqpAdmin amqpAdmin, IRuntimeTenantResolver pThreadTenantResolver,
            RabbitTemplate rabbitTemplate) {
        return new InstancePublisher(rabbitTemplate, rabbitAdmin, amqpAdmin, pRabbitVirtualHostAdmin);
    }

    @Bean
    public IInstanceSubscriber instanceSubscriber(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            MessageConverter jsonMessageConverters, ITenantResolver pTenantResolver, RegardsErrorHandler errorHandler,
            IRuntimeTenantResolver runtimeTenantResolver, IInstancePublisher instancePublisher, IPublisher publisher) {
        return new InstanceSubscriber(pRabbitVirtualHostAdmin, amqpAdmin, jsonMessageConverters, errorHandler,
                microserviceName, instancePublisher, publisher, runtimeTenantResolver);
    }

    @Bean
    public IInstancePoller instancePoller(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin, IAmqpAdmin amqpAdmin,
            IRuntimeTenantResolver pThreadTenantResolver, RabbitTemplate rabbitTemplate) {
        return new InstancePoller(pRabbitVirtualHostAdmin, rabbitTemplate, amqpAdmin);
    }

    @Bean
    public MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory() {
        return new MultitenantSimpleRoutingConnectionFactory();
    }

    /**
     * This bean is only useful if no {@link PlatformTransactionManager} was provided by a database or else.
     * @param pThreadTenantResolver runtime tenant resolver
     * @param pRabbitVirtualHostAdmin virtual host admin
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
    @Profile("!nohandler")
    public AmqpEventHandler amqpEventHandler(IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin,
            IInstanceSubscriber pInstanceSubscriber, ISubscriber pSubscriber) {
        return new AmqpEventHandler(pRabbitVirtualHostAdmin, pInstanceSubscriber, pSubscriber);
    }
}
