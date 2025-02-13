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
package fr.cnes.regards.modules.retry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;

/**
 * Retry configuration with a custom retryListener which logs error thrown in the retried methods, with an error count.
 * By default, errors in retryable methods are not logged, and it is difficult to detect if a retry has occurred.</br>
 * To use it, <b>simply add the regards-retry dependency in your module</b>. The RetryTemplate will be automagically injected.
 *
 * @author tguillou
 */
@Configuration
@EnableRetry
public class RegardsRetryConfig {

    @Bean
    public RetryListener regardsRetryListener() {
        return new RegardsRetryListener();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();
        template.registerListener(new RegardsRetryListener());
        return template;
    }
}
