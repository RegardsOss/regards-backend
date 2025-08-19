/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.crawler.service.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration class to create a thread pool for Elasticsearch saves operations.
 *
 * @author tguillou
 */
@Configuration
public class ESThreadPoolConfiguration {

    /**
     * Thread pool size for Elasticsearch bulk operations.
     * Recommended: up to 5 client threads per Elasticsearch node.
     * On the server side, the Elasticsearch thread pool usually has 2 to 4 threads per node, for 'write' operations.
     */
    @Value("${es.thread.pool.size:5}")
    private int threadPoolSize;

    /**
     * Note : this thread pool reserve all threads even if they are not used.
     */
    @Bean
    public ExecutorService esThreadPool() {
        // fixed thread pool alloc all threads at startup, to reserve them for Elasticsearch operations
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}
