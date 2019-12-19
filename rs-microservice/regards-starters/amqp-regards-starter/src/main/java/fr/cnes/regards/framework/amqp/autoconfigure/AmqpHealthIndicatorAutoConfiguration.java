/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.AmqpHealthIndicator;

/**
 * Adapted Rabbit Health indicator
 *
 * @author Marc SORDI
 *
 * FIXME : WIP with spring boot admin
 *
 */
@Configuration
public class AmqpHealthIndicatorAutoConfiguration {

    /**
     * bean providing properties from the configuration file
     */
    @Autowired
    private RabbitProperties rabbitProperties;

    // Override RabbitMQ health indicator
    @Bean(name = "rabbitHealthIndicator")
    @ConditionalOnMissingBean(name = "rabbitHealthIndicator")
    public HealthIndicator rabbitHealthIndicator() {
        return new AmqpHealthIndicator(rabbitProperties);
    }
}
