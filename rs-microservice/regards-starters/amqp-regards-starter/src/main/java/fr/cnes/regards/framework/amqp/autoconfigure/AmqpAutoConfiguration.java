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

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.*;
import fr.cnes.regards.framework.amqp.configuration.*;
import fr.cnes.regards.framework.amqp.converter.Gson2JsonMessageConverter;
import fr.cnes.regards.framework.amqp.converter.JsonMessageConverters;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.single.SingleVhostPoller;
import fr.cnes.regards.framework.amqp.single.SingleVhostPublisher;
import fr.cnes.regards.framework.amqp.single.SingleVhostSubscriber;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantBootstrapProperties;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AsyncConsumerStoppedEvent;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper.TypePrecedence;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Automatic configuration class for RabbitMQ (amqp protocol)
 *
 * @author svissier
 */
@AutoConfiguration(after = { GsonAutoConfiguration.class })
@ConditionalOnProperty(prefix = "regards.amqp", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({ RabbitProperties.class,
                                 AmqpManagementProperties.class,
                                 AmqpMicroserviceProperties.class,
                                 NotifierEventsProperties.class,
                                 RetryProperties.class })
@EnableTransactionManagement
public class AmqpAutoConfiguration {

    @Autowired
    private MultitenantBootstrapProperties bootstrapProperties;

    /**
     * bean providing properties from the configuration file
     * Be careful: on this object, only 4 properties are really used,
     * the rest is completely ignored
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
     * bean providing properties from the retry configuration
     */
    @Autowired
    private RetryProperties retryProperties;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Gson gson;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${regards.instance.name:REGARDS}")
    private String applicationId;

    /**
     * Default number of retries after passive queue declaration fails.
     */
    @Value("${regards.rabbitmq.number.retries.failed.queue:3}")
    private int declarationRetries;

    /**
     * Default interval between failed queue declaration attempts in milliseconds.
     */
    @Value("${regards.rabbitmq.interval.retries.failed.queue:5000}")
    private long failedDeclarationRetryInterval;

    /**
     * Maximum body length to print Message body as String, see {@link Message#setMaxBodyLength(int)}
     */
    @Value("${regards.rabbitmq.max.body.length.render:1000}")
    private int maxBodyLengthStringRender;

    /**
     * List of events which will also be sent to rs-notifier after publishing the original event
     */
    @Autowired
    private NotifierEventsProperties notifierEventsProperties;

    @Bean
    @ConditionalOnMissingBean(IRabbitVirtualHostAdmin.class)
    public IRabbitVirtualHostAdmin rabbitVirtualHostAdmin(ITenantResolver tenantResolver,
                                                          final MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory,
                                                          final RestOperations restOperations) {
        return new RabbitVirtualHostAdmin(amqpManagmentProperties.getMode(),
                                          tenantResolver,
                                          rabbitProperties.getUsername(),
                                          rabbitProperties.getPassword(),
                                          amqpManagmentProperties.getProtocol(),
                                          amqpManagmentProperties.getHost(),
                                          amqpManagmentProperties.getPort(),
                                          restOperations,
                                          simpleRoutingConnectionFactory,
                                          rabbitProperties.determineAddresses(),
                                          rabbitProperties.getSsl().getEnabled(),
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
        return new RegardsAmqpAdmin(amqpManagmentProperties.getNamespace(),
                                    amqpMicroserviceProperties.getTypeIdentifier(),
                                    amqpMicroserviceProperties.getInstanceIdentifier());
    }

    @Bean
    public RegardsErrorHandler errorHandler(IRuntimeTenantResolver runtimeTenantResolver,
                                            IInstancePublisher instancePublisher,
                                            IPublisher publisher) {
        return new RegardsErrorHandler(runtimeTenantResolver, instancePublisher, publisher, microserviceName);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(final SimpleRoutingConnectionFactory simpleRoutingConnectionFactory) {
        return new RabbitAdmin(simpleRoutingConnectionFactory);
    }

    @Bean
    public RabbitTemplate transactionalRabbitTemplate(MessageConverter jsonMessageConverters,
                                                      MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(simpleRoutingConnectionFactory);
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

        // Configure toString render
        Message.setMaxBodyLength(maxBodyLengthStringRender);

        return converters;
    }

    @Bean
    public IPublisher publisher(IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                                RabbitAdmin rabbitAdmin,
                                IAmqpAdmin amqpAdmin,
                                IRuntimeTenantResolver threadTenantResolver,
                                RabbitTemplate rabbitTemplate) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Publisher(applicationId,
                                 rabbitVirtualHostAdmin,
                                 rabbitTemplate,
                                 rabbitAdmin,
                                 amqpAdmin,
                                 threadTenantResolver,
                                 gson,
                                 notifierEventsProperties.getEvents());
        } else {
            return new SingleVhostPublisher(applicationId,
                                            rabbitTemplate,
                                            rabbitAdmin,
                                            amqpAdmin,
                                            rabbitVirtualHostAdmin,
                                            threadTenantResolver,
                                            gson,
                                            notifierEventsProperties.getEvents());
        }
    }

    @Bean
    public ISubscriber subscriber(IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                                  IAmqpAdmin amqpAdmin,
                                  MessageConverter jsonMessageConverters,
                                  ITenantResolver tenantResolver,
                                  RegardsErrorHandler errorHandler,
                                  IRuntimeTenantResolver runtimeTenantResolver,
                                  IInstancePublisher instancePublisher,
                                  IPublisher publisher,
                                  ApplicationEventPublisher applicationEventPublisher,
                                  RabbitTemplate rabbitTemplate,
                                  TransactionTemplate transactionTemplate) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Subscriber(rabbitVirtualHostAdmin,
                                  rabbitTemplate,
                                  amqpAdmin,
                                  jsonMessageConverters,
                                  tenantResolver,
                                  errorHandler,
                                  microserviceName,
                                  instancePublisher,
                                  publisher,
                                  runtimeTenantResolver,
                                  applicationEventPublisher,
                                  declarationRetries,
                                  failedDeclarationRetryInterval,
                                  retryProperties,
                                  transactionTemplate);
        } else {
            return new SingleVhostSubscriber(rabbitVirtualHostAdmin,
                                             rabbitTemplate,
                                             amqpAdmin,
                                             jsonMessageConverters,
                                             tenantResolver,
                                             errorHandler,
                                             microserviceName,
                                             instancePublisher,
                                             publisher,
                                             runtimeTenantResolver,
                                             applicationEventPublisher,
                                             declarationRetries,
                                             failedDeclarationRetryInterval,
                                             retryProperties,
                                             transactionTemplate);
        }
    }

    @Bean
    public IPoller poller(IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                          IAmqpAdmin amqpAdmin,
                          IRuntimeTenantResolver threadTenantResolver,
                          RabbitTemplate rabbitTemplate) {
        if (VirtualHostMode.MULTI.equals(amqpManagmentProperties.getMode())) {
            return new Poller(rabbitVirtualHostAdmin, rabbitTemplate, amqpAdmin, threadTenantResolver);
        } else {
            return new SingleVhostPoller(rabbitVirtualHostAdmin, rabbitTemplate, amqpAdmin, threadTenantResolver);
        }
    }

    @Bean
    public IInstancePublisher instancePublisher(IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                                                RabbitAdmin rabbitAdmin,
                                                IAmqpAdmin amqpAdmin,
                                                RabbitTemplate rabbitTemplate) {
        return new InstancePublisher(applicationId,
                                     rabbitTemplate,
                                     rabbitAdmin,
                                     amqpAdmin,
                                     rabbitVirtualHostAdmin,
                                     gson,
                                     notifierEventsProperties.getEvents());
    }

    @Bean
    public IInstanceSubscriber instanceSubscriber(IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                                                  IAmqpAdmin amqpAdmin,
                                                  MessageConverter jsonMessageConverters,
                                                  RegardsErrorHandler errorHandler,
                                                  IRuntimeTenantResolver runtimeTenantResolver,
                                                  IInstancePublisher instancePublisher,
                                                  IPublisher publisher,
                                                  ApplicationEventPublisher applicationEventPublisher,
                                                  RabbitTemplate rabbitTemplate,
                                                  TransactionTemplate transactionTemplate) {
        return new InstanceSubscriber(rabbitVirtualHostAdmin,
                                      rabbitTemplate,
                                      transactionTemplate,
                                      amqpAdmin,
                                      jsonMessageConverters,
                                      errorHandler,
                                      microserviceName,
                                      instancePublisher,
                                      publisher,
                                      runtimeTenantResolver,
                                      applicationEventPublisher,
                                      declarationRetries,
                                      failedDeclarationRetryInterval,
                                      retryProperties);
    }

    @Bean
    public IInstancePoller instancePoller(IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                                          IAmqpAdmin amqpAdmin,
                                          RabbitTemplate rabbitTemplate) {
        return new InstancePoller(rabbitVirtualHostAdmin, rabbitTemplate, amqpAdmin);
    }

    @Bean
    public MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory() {
        return new MultitenantSimpleRoutingConnectionFactory();
    }

    /**
     * This bean is only useful if no {@link PlatformTransactionManager} was provided by a database or else.
     *
     * @param threadTenantResolver   runtime tenant resolver
     * @param rabbitVirtualHostAdmin virtual host admin
     * @return a {@link RabbitTransactionManager}
     */
    @Bean
    @ConditionalOnProperty(prefix = "regards.amqp", name = "internal.transaction", matchIfMissing = false)
    public PlatformTransactionManager rabbitTransactionManager(IRuntimeTenantResolver threadTenantResolver,
                                                               IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                                                               MultitenantSimpleRoutingConnectionFactory simpleRoutingConnectionFactory) {
        return new MultitenantRabbitTransactionManager(amqpManagmentProperties.getMode(),
                                                       simpleRoutingConnectionFactory,
                                                       threadTenantResolver,
                                                       rabbitVirtualHostAdmin);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    @Profile("!nohandler")
    public AmqpEventHandler amqpEventHandler(IRabbitVirtualHostAdmin rabbitVirtualHostAdmin,
                                             IInstanceSubscriber instanceSubscriber,
                                             ISubscriber subscriber) {
        return new AmqpEventHandler(rabbitVirtualHostAdmin, instanceSubscriber, subscriber);
    }

    /**
     * Stop the entire application when some rabbit consumer stops. The program terminates with a 0 return code.
     */
    @Bean
    @Profile("!test")
    public ApplicationListener<AsyncConsumerStoppedEvent> createAsyncConsumerStoppedEventListener() {
        return event -> {
            if (event.getConsumer() instanceof BlockingQueueConsumer blockingQueueConsumer
                && blockingQueueConsumer.isNormalCancel()) {
                // ignore normal cancel as the software can reconnect to it safely
                // For example, it happens when there is a new tenant, consumer is
                // cancel "normally" (or expected) then recreated
                return;
            }
            System.exit(SpringApplication.exit(context, () -> 0));
        };
    }

}
