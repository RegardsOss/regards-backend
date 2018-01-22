/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.microservices.administration;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;

/**
 *
 * Class JpaTenantConnectionConfiguration
 *
 * Test configuration class
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ComponentScan("fr.cnes.regards.modules")
@PropertySource("classpath:application-test.properties")
@EnableAutoConfiguration(exclude = LocalAuthoritiesProviderAutoConfiguration.class)
public class AuthoritiesTestConfiguration {

    /**
     * Test project name
     */
    public static final String PROJECT_NAME = "new-test-project";

    /**
     * Role name with access granted to CORS requests.
     */
    public static final String ROLE_NAME = "USER_ROLE";

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    @Bean
    public IAuthenticationResolver mockAuthenticationResolver() {
        return Mockito.mock(IAuthenticationResolver.class);
    }

    /**
     *
     * Initialize a Mock for AMQP Publisher
     *
     * @return IPublisher
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IPublisher mockPublisher() {
        return Mockito.mock(IPublisher.class);
    }

    @Bean
    public IInstancePublisher mockInstancePublisher() {
        return Mockito.mock(IInstancePublisher.class);
    }

    /**
     *
     * Initialize a Mock for AMQP Subsriber
     *
     * @return ISubscriber
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ISubscriber mockSubscriber() {
        return Mockito.mock(ISubscriber.class);
    }

    @Bean
    public IInstanceSubscriber mockInstanceSubscriber() {
        return Mockito.mock(IInstanceSubscriber.class);
    }
}
