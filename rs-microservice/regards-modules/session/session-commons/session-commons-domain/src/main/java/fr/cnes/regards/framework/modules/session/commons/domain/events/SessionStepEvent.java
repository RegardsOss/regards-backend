/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.commons.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;

import java.util.Objects;

/**
 * Event to update Session
 *
 * @author Iliana Ghazali
 **/

@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class SessionStepEvent implements ISubscribable {

    /**
     * Session step to be stored
     */
    private SessionStep sessionStep;

    public SessionStepEvent(SessionStep sessionStep) {
        this.sessionStep = sessionStep;
    }

    public SessionStepEvent() {
    }

    public SessionStep getSessionStep() {
        return sessionStep;
    }

    @Override
    public String toString() {
        return "SessionStepEvent{" + "sessionStep=" + sessionStep + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SessionStepEvent that = (SessionStepEvent) o;
        return Objects.equals(sessionStep, that.sessionStep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionStep);
    }
}
