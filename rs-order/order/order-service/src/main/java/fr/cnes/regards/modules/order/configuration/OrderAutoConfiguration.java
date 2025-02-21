/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.configuration;

import fr.cnes.regards.modules.order.domain.basket.Basket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Auto configuration for the Order services.
 *
 * @author Arnaud Bos
 */
@Configuration
public class OrderAutoConfiguration {

    @Value("${regards.order.async.creation.concurrent.limit:2}")
    private int maxConcurrentLimit;

    @Value("${regards.order.async.creation.concurrent.queue.capacity:500}")
    private int maxQueueCapacity;

    /**
     * Creates a specific thread pool to manage order completion process.
     *
     * @see fr.cnes.regards.modules.order.service.OrderCreationService#asyncCompleteOrderCreation(Basket, String, Long, int, String, String)
     */
    @Bean(name = "orderThreadPoolTaskExecutor")
    @Primary
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(2);
        threadPoolTaskExecutor.setMaxPoolSize(maxConcurrentLimit);
        threadPoolTaskExecutor.setQueueCapacity(maxQueueCapacity);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

}
