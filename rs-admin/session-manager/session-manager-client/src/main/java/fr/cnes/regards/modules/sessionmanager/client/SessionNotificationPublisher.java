/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
    public static final String SESSION_TRACKED_ELEMENT_TEMPLATE = "Session tracked element {} = {}";

    @Value("${spring.application.name}")
    private String monitoringStep;

    @Autowired
    private IPublisher publisher;

    private final Map<String, Long> notifCache = new ConcurrentHashMap<>();

    private final Map<String, String> stateCache = new ConcurrentHashMap<>();

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
        notifCache.merge(key, value, Long::sum);
        LOGGER.trace(SESSION_TRACKED_ELEMENT_TEMPLATE, key, notifCache.get(key));
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
        notifCache.merge(key, -value, Long::sum);
        if (notifCache.get(key) < 0) {
            // Value may be negative after restart!
            LOGGER.warn(SESSION_TRACKED_ELEMENT_TEMPLATE, key, notifCache.get(key));
        } else {
            LOGGER.trace(SESSION_TRACKED_ELEMENT_TEMPLATE, key, notifCache.get(key));
        }
    }

    @Override
    public void stepValue(String sessionOwner, String session, String property, SessionNotificationState notifState,
            String value) {
        SessionMonitoringEvent event = SessionMonitoringEvent.build(sessionOwner, session, notifState, monitoringStep,
                                                                    SessionNotificationOperator.REPLACE, property,
                                                                    value);
        publisher.publish(event);

        // Cache
        String key = getCacheKey(sessionOwner, session, property, notifState);
        stateCache.put(key, notifCache.get(key) + value);
        LOGGER.trace(SESSION_TRACKED_ELEMENT_TEMPLATE, key, stateCache.get(key));
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

    public void clearCache() {
        notifCache.clear();
        stateCache.clear();
    }

    /**
     * Make a dump of the cache value
     */
    public void debugSession() {
        for (Entry<String, Long> entry : notifCache.entrySet()) {
            LOGGER.debug("[CACHE DUMP / counter] Session tracked element {} = {}", entry.getKey(), entry.getValue());
        }
        for (Entry<String, String> entry : stateCache.entrySet()) {
            LOGGER.debug("[CACHE DUMP / state] Session tracked element {} = {}", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Assert that counter has expected count!
     */
    public boolean assertCount(String sessionOwner, String session, String property,
            SessionNotificationState notifState, Long expectedCount) {
        String key = getCacheKey(sessionOwner, session, property, notifState);
        Long count = notifCache.get(key);
        boolean result = ((count == null) && (expectedCount == null))
                || ((count != null) && count.equals(expectedCount));

        // Debug
        if (result) {
            LOGGER.debug("Session tracked element {} : accurate expected count \"{}\"", key, expectedCount);
        } else {
            LOGGER.error("Session tracked element {} : expected count \"{}\" but was \"{}\"", key, expectedCount,
                         count);
        }
        return result;
    }

    /**
     * Assert that state has expected one!
     */
    public boolean assertState(String sessionOwner, String session, String property,
            SessionNotificationState notifState, String expectedState) {
        String key = getCacheKey(sessionOwner, session, property, notifState);
        String state = stateCache.get(key);
        boolean result = (state != null) && state.equals(expectedState);

        // Debug
        if (result) {
            LOGGER.debug("Session tracked element {} : accurate expected state \"{}\"", key, expectedState);
        } else {
            LOGGER.error("Session tracked element {} : expected state \"{}\" but was \"{}\"", key, expectedState,
                         state);
        }
        return result;
    }
}
