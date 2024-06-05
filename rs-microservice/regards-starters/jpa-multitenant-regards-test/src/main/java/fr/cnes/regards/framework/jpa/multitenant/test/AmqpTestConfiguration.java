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
package fr.cnes.regards.framework.jpa.multitenant.test;

import java.util.Optional;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.utils.spring.CglibHelper;

/**
 * Provide during test either empty beans (when profile="!testAmqp") relative to AMQP
 * or a ISubscriber that cleans queue before subscribing (when profile="testAmqp")
 *
 * @author Léo Mieulet
 */
@Configuration
public class AmqpTestConfiguration {

    /**
     * Override the default {@link ISubscriber} to ensure that all methods running {@link ISubscriber#subscribeTo}
     * will clean the queue before subscribing.
     * When this bean is effective, you don't need to execute {@link ISubscriber#purgeAllQueues(String)}
     * as this is already done
     *
     * @return {@link ISubscriber}
     */
    @Bean
    @Primary
    @Profile("testAmqp")
    public ISubscriber subscriberMock(ISubscriber subscriber) {
        return new ISubscriber() {

            @Override
            public void addTenant(String tenant) {
                subscriber.addTenant(tenant);
            }

            @Override
            public void removeTenant(String tenant) {
                subscriber.removeTenant(tenant);
            }

            @Override
            public <E extends ISubscribable> void subscribeTo(Class<E> eventType, IHandler<E> receiver) {
                subscriber.subscribeTo(eventType, receiver, true);
            }

            @Override
            public <E extends ISubscribable> void subscribeTo(Class<E> eventType,
                                                              IHandler<E> receiver,
                                                              String queueName,
                                                              String exchangeName) {
                subscriber.subscribeTo(eventType, receiver, queueName, exchangeName, true);
            }

            @Override
            public <E extends ISubscribable> void subscribeTo(Class<E> eventType,
                                                              IHandler<E> receiver,
                                                              String queueName,
                                                              String exchangeName,
                                                              boolean purgeQueue) {
                subscriber.subscribeTo(eventType, receiver, queueName, exchangeName, true);
            }

            @Override
            public <E extends ISubscribable> void subscribeTo(Class<E> eventType,
                                                              IHandler<E> receiver,
                                                              boolean purgeQueue) {
                subscriber.subscribeTo(eventType, receiver, true);
            }

            @Override
            public <T extends ISubscribable> void unsubscribeFrom(Class<T> eventType, boolean fast) {
                subscriber.unsubscribeFrom(eventType, fast);
            }

            @Override
            public void unsubscribeFromAll(boolean fast) {
                subscriber.unsubscribeFromAll(fast);
            }

            @Override
            public void purgeAllQueues(String tenant) {
                subscriber.purgeAllQueues(tenant);
            }

            @Override
            public <E extends ISubscribable> void purgeQueue(Class<E> eventType,
                                                             Class<? extends IHandler<E>> handlerType,
                                                             Optional<String> queueName) {
                subscriber.purgeQueue(eventType, handlerType, queueName);
            }
        };
    }

    /**
     * Subscriber mock
     *
     * @return {@link ISubscriber}
     */
    @Bean
    @Primary
    @Profile("!testAmqp")
    public ISubscriber eventSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    /**
     * Instance subscriber mock
     *
     * @return {@link IInstanceSubscriber}
     */
    @Bean
    @Primary
    @Profile("!testAmqp")
    public IInstanceSubscriber eventInstanceSubscriber() {
        return Mockito.mock(IInstanceSubscriber.class);
    }

    /**
     * Publisher mock
     */
    @Bean
    @Primary
    @Profile("!testAmqp")
    public IPublisher eventPublisherMock() {
        return Mockito.mock(IPublisher.class);
    }

    /**
     * Because a mock() is returned when no profile testAmqp, it is necessary to return a spy() of "true" publisher (to be consistent).
     * This avoids using @SpyBean on IPublisher under tests (Mockito doesn't like spying a mock) for the benefit of @Autowired
     */
    @Bean
    @Primary
    @Profile("testAmqp")
    public IPublisher eventPublisherSpy(IPublisher publisher) {
        // Don't spy an autowired object, spy proxied object instead
        return Mockito.spy(CglibHelper.getTargetObject(publisher));
    }

    /**
     * Instance publisher mock
     */
    @Bean
    @Primary
    @Profile("!testAmqp")
    public IInstancePublisher eventInstancePublisher() {
        return Mockito.mock(IInstancePublisher.class);
    }
}
