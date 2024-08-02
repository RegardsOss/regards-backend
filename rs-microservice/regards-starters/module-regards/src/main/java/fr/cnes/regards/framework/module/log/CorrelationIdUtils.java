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

package fr.cnes.regards.framework.module.log;

import org.slf4j.MDC;

/**
 * @author Theo Lasserre
 */
public final class CorrelationIdUtils {

    private static final String CORRELATION_ID = "correlationId";

    private CorrelationIdUtils() {
    }

    /**
     * Set correlation id to a specific value on current thread.<br/>
     * We recommend to use {@link CorrelationIdUtils#clearCorrelationId()} to clean the thread in a finally clause.<br/>
     * It is mostly recommended for server threads as they are reused.
     *
     * @param correlationId correlationId
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
    }

    /**
     * Clear forced correlation id on current thread.<br>
     */
    public static void clearCorrelationId() {
        MDC.remove(CORRELATION_ID);
    }
}
