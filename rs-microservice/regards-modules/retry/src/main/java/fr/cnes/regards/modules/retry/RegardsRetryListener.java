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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

/**
 * Retry listener that logs errors thrown in the retried methods. The logs display the error count and the method name.
 *
 * @author tguillou
 * @see RegardsRetryConfig
 */
class RegardsRetryListener implements RetryListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsRetryListener.class);

    @Override
    public <T, E extends Throwable> void onError(RetryContext context,
                                                 RetryCallback<T, E> callback,
                                                 Throwable throwable) {
        int retryCount = context.getRetryCount();
        try {
            Object retryMaxAttempt = context.getAttribute("context.max-attempts");
            Object methodName = context.getAttribute("context.name");
            LOGGER.warn("Retry {}/{} on service {} after error : {}",
                        retryCount,
                        retryMaxAttempt,
                        methodName,
                        throwable.getMessage());
        } catch (Throwable e) {
            LOGGER.warn("Retry {} after error : ", retryCount, throwable);
        }

    }
}