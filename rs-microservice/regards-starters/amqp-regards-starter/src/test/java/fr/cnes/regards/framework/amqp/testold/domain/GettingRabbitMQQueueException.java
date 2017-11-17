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
package fr.cnes.regards.framework.amqp.testold.domain;

/**
 * @author svissier
 *
 */
@SuppressWarnings("serial")
public class GettingRabbitMQQueueException extends Exception {

    /**
     *
     */
    public GettingRabbitMQQueueException() {
    }

    /**
     * @param pMessage
     */
    public GettingRabbitMQQueueException(String pMessage) {
        super(pMessage);
    }

    /**
     * @param pCause
     */
    public GettingRabbitMQQueueException(Throwable pCause) {
        super(pCause);
    }

    /**
     * @param pMessage
     * @param pCause
     */
    public GettingRabbitMQQueueException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    /**
     * @param pMessage
     * @param pCause
     * @param pEnableSuppression
     * @param pWritableStackTrace
     */
    public GettingRabbitMQQueueException(String pMessage, Throwable pCause, boolean pEnableSuppression,
            boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
    }

}
