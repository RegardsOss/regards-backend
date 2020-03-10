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
package fr.cnes.regards.modules.sessionmanager.client;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

@Service
public class SessionNotificationPublisher implements ISessionNotificationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionNotificationPublisher.class);

    private static final String KEY_SEPARATOR = "#";

    @Value("${spring.application.name}")
    private String monitoringStep;

    @Autowired
    private IPublisher publisher;

    private final Map<String, Long> notifCache = new ConcurrentHashMap<>();

    @Override
    public void setStep(String step) {
        this.monitoringStep = step;
    }

    @Override
    public void increment(String sessionOwner, String session, String property, SessionNotificationState notifState,
            long value) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(sessionOwner, session, notifState, monitoringStep,
                                                                    SessionNotificationOperator.INC, property, value);
        publisher.publish(event);

        // Cache
        String key = getCacheKey(sessionOwner, session, property, notifState);
        if (notifCache.containsKey(key)) {
            notifCache.put(key, notifCache.get(key) + value);
        } else {
            notifCache.put(key, value);
        }
        LOGGER.trace("Session tracked element {} = {}", key, notifCache.get(key));
    }

    @Override
    public void decrement(String sessionOwner, String session, String property, SessionNotificationState notifState,
            long value) {
        // Add one to the new state
        SessionMonitoringEvent event = SessionMonitoringEvent.build(sessionOwner, session, notifState, monitoringStep,
                                                                    SessionNotificationOperator.DEC, property, value);
        publisher.publish(event);

        // Cache
        String key = getCacheKey(sessionOwner, session, property, notifState);
        if (notifCache.containsKey(key)) {
            notifCache.put(key, notifCache.get(key) - value);
        } else {
            notifCache.put(key, value);
        }
        if (notifCache.get(key) < 0) {
            // Value may be negative after restart!
            LOGGER.warn("Session tracked element {} = {}", key, notifCache.get(key));
        } else {
            LOGGER.trace("Session tracked element {} = {}", key, notifCache.get(key));
        }
    }

    @Override
    public void stepValue(String sessionOwner, String session, String property, SessionNotificationState notifState,
            String value) {
        SessionMonitoringEvent event = SessionMonitoringEvent.build(sessionOwner, session, notifState, monitoringStep,
                                                                    SessionNotificationOperator.REPLACE, property,
                                                                    value);
        publisher.publish(event);
        String key = getCacheKey(sessionOwner, session, property, notifState);
        LOGGER.trace("Session tracked element {} = {}", key, value);
    }

    private String getCacheKey(String sessionOwner, String session, String property,
            SessionNotificationState notifState) {
        StringBuilder builder = new StringBuilder();
        builder.append(sessionOwner);
        builder.append(KEY_SEPARATOR);
        builder.append(session);
        builder.append(KEY_SEPARATOR);
        builder.append(property);
        builder.append(KEY_SEPARATOR);
        builder.append(notifState);
        return builder.toString();
    }

    /**
     * Make a dump of the cache value
     */
    public void debugSession() {
        for (Entry<String, Long> entry : notifCache.entrySet()) {
            LOGGER.debug("[CACHE DUMP] Session tracked element {} = {}", entry.getKey(), entry.getValue());
        }
    }
}
