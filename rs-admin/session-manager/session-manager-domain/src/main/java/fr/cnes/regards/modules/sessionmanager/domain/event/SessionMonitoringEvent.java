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
package fr.cnes.regards.modules.sessionmanager.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class SessionMonitoringEvent implements ISubscribable {
    /**
     * The source of the session
     */
    String source;
    /**
     * The name of the session
     */
    String name;
    /**
     * The state of the notification (error, success)
     */
    SessionNotificationState state;
    /**
     * The name of the step this notification comes from
     */
    String step;
    /**
     * The type of operator we use to update the session property
     */
    SessionNotificationOperator operator;
    /**
     * The property key we want to update
     */
    String property;
    /**
     * The value to use
     */
    Object value;

    private boolean global = false;

    public static SessionMonitoringEvent build(String source, String name, SessionNotificationState state, String step, SessionNotificationOperator operator, String property, String value) {
        SessionMonitoringEvent sessionMonitoringEvent = build(source, name, state, step, operator, property);
        sessionMonitoringEvent.setValue(value);
        return sessionMonitoringEvent;
    }

    public static SessionMonitoringEvent buildGlobal(SessionNotificationState state, String step, SessionNotificationOperator operator, String property, String value) {
        SessionMonitoringEvent sessionMonitoringEvent = build(null, null, state, step, operator, property);
        sessionMonitoringEvent.setValue(value);
        sessionMonitoringEvent.setGlobal(true);
        return sessionMonitoringEvent;
    }

    public static SessionMonitoringEvent build(String source, String name, SessionNotificationState state, String step, SessionNotificationOperator operator, String property, long value) {
        SessionMonitoringEvent sessionMonitoringEvent = build(source, name, state, step, operator, property);
        sessionMonitoringEvent.setValue(value);
        return sessionMonitoringEvent;
    }

    public static SessionMonitoringEvent buildGlobal(SessionNotificationState state, String step, SessionNotificationOperator operator, String property, long value) {
        SessionMonitoringEvent sessionMonitoringEvent = build(null, null, state, step, operator, property);
        sessionMonitoringEvent.setValue(value);
        sessionMonitoringEvent.setGlobal(true);
        return sessionMonitoringEvent;
    }

    private static SessionMonitoringEvent build(String source, String name, SessionNotificationState state, String step, SessionNotificationOperator operator, String property) {
        SessionMonitoringEvent sessionMonitoringEvent = new SessionMonitoringEvent();
        sessionMonitoringEvent.setName(name);
        sessionMonitoringEvent.setSource(source);
        sessionMonitoringEvent.setState(state);
        sessionMonitoringEvent.setStep(step);
        sessionMonitoringEvent.setOperator(operator);
        sessionMonitoringEvent.setProperty(property);
        return sessionMonitoringEvent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public SessionNotificationState getState() {
        return state;
    }

    public void setState(SessionNotificationState state) {
        this.state = state;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public SessionNotificationOperator getOperator() {
        return operator;
    }

    public void setOperator(SessionNotificationOperator operator) {
        this.operator = operator;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }
}
