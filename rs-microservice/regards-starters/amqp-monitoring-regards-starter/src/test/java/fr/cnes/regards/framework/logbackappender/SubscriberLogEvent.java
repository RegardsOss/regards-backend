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
package fr.cnes.regards.framework.logbackappender;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;

/**
 * This class is used to store the {@link LogEvent} received by the subscriber.
 * @author Christophe Mertz
 */
@Component
public class SubscriberLogEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberLogEvent.class);

    private List<TenantWrapper<LogEvent>> wrappers = new ArrayList<>();

    public void addLogEvent(TenantWrapper<LogEvent> logEvent) {
        wrappers.add(logEvent);
    }

    public List<TenantWrapper<LogEvent>> getMessages() {
        return wrappers;
    }

    public void reset() {
        wrappers.clear();
    }
}
