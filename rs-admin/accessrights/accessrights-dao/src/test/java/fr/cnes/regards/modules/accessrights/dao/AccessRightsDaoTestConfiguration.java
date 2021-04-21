/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.dao;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;

/**
 *
 * Class AccessRightsDaoTestConfiguration
 *
 * Test Configuration class
 *
 * @author CS

 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests-embedded.properties")
public class AccessRightsDaoTestConfiguration {

    /**
     *
     * Mock AMQP
     *
     * @return {@link IPublisher}

     */
    @Bean
    public IPublisher eventPublisher() {
        return Mockito.mock(IPublisher.class);
    }

    /**
     *
     * Mock AMQP
     *
     * @return {@link ISubscriber}

     */
    @Bean
    public ISubscriber eventSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    /**
     *
     * Mock AMQP
     *
     * @return {@link IPublisher}

     */
    @Bean
    public IInstanceSubscriber eventInstanceSubscriber() {
        return Mockito.mock(IInstanceSubscriber.class);
    }

    /**
     *
     * Mock AMQP
     *
     * @return {@link IPublisher}

     */
    @Bean
    public IInstancePublisher eventInstancePublisher() {
        return Mockito.mock(IInstancePublisher.class);
    }
}
