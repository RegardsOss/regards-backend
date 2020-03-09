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
package fr.cnes.regards.modules.acquisition.service;

import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;

/**
 * @author sbinda
 *
 */
@Component
public class SessionNotificationHandler
        implements IHandler<SessionMonitoringEvent>, ApplicationListener<ApplicationReadyEvent> {

    List<SessionMonitoringEvent> events = Lists.newArrayList();

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        // Subscribe to events on {@link StorageDataFile} changes.
        subscriber.subscribeTo(SessionMonitoringEvent.class, this);
    }

    @Override
    public void handle(TenantWrapper<SessionMonitoringEvent> wrapper) {
        events.add(wrapper.getContent());
        this.log(wrapper.getContent());
    }

    /**
     * @param content
     */
    private void log(SessionMonitoringEvent event) {
        LOGGER.info("[{}] {}.{}={} {}", event.getState(), event.getStep(), event.getProperty(), event.getOperator(),
                    event.getValue());
    }

    public int getPropertyCount(String step, String property, SessionNotificationOperator op) {
        return events.stream()
                .filter(e -> (op == e.getOperator()) && e.getStep().equals(step) && e.getProperty().equals(property))
                .map(event -> (Integer) event.getValue()).reduce(0, (a, b) -> {
                    return a + b;
                });
    }

    public void clear() {
        events.clear();
    }

}
