/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.logbackappender.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;

/**
 * This class is used to subscribe to the {@link LogEvent} send by the REGARDS microservice
 * @author Christophe Mertz
 */
public class MonitoringLogEvent implements IMonitoringLogEvent {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringLogEvent.class);

    public MonitoringLogEvent(ISubscriber subscriber, IHandler<LogEvent> handler) {
        super();
        LOG.debug("Subscription to logEvent queue");
        subscriber.subscribeTo(LogEvent.class, handler);
    }

}
