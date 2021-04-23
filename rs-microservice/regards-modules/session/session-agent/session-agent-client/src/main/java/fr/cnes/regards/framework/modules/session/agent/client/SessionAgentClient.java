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
package fr.cnes.regards.framework.modules.session.agent.client;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Client to publish new {@link StepPropertyUpdateRequestEvent}. These events will then be used to create
 * {@link SessionStep}.
 *
 * @author Iliana Ghazali
 */
@Component
public class SessionAgentClient {

    /**
     * Publisher to publish {@link StepPropertyUpdateRequestEvent}
     */
    @Autowired
    private IPublisher publisher;

    /**
     * Send a {@link StepPropertyUpdateRequestEvent} with a {@link StepPropertyEventTypeEnum} of type increment. The
     * corresponding property in the session step will be incremented with the value provided.
     */
    public void increment(String stepId, String source, String session, StepPropertyEventInfo stepPropertyEventInfo) {
        // Create new event
        StepPropertyUpdateRequestEvent stepPropertyEvent = new StepPropertyUpdateRequestEvent(stepId, source, session,
                                                                                              StepPropertyEventTypeEnum.INC,
                                                                                              stepPropertyEventInfo);
        // Publish event
        publisher.publish(stepPropertyEvent);
    }

    /**
     * Send a {@link StepPropertyUpdateRequestEvent} with a {@link StepPropertyEventTypeEnum} of type decrement. The
     * corresponding property in the session step will be decremented with the value provided.
     */
    public void decrement(String stepId, String source, String session, StepPropertyEventInfo stepPropertyEventInfo) {
        // Create new event
        StepPropertyUpdateRequestEvent stepPropertyEvent = new StepPropertyUpdateRequestEvent(stepId, source, session,
                                                                                              StepPropertyEventTypeEnum.DEC,
                                                                                              stepPropertyEventInfo);
        // Publish event
        publisher.publish(stepPropertyEvent);
    }

    /**
     * Send a {@link StepPropertyUpdateRequestEvent} with a {@link StepPropertyEventTypeEnum} of type increment. The
     * corresponding property in the session step will be set to the value provided.
     */
    public void stepValue(String stepId, String source, String session, StepPropertyEventInfo stepPropertyEventInfo)
            throws EntityInvalidException {
        // check if input and output are false
        if (stepPropertyEventInfo.isInputRelated() || stepPropertyEventInfo.isOutputRelated()) {
            String msg = String
                    .format("Step property with source \"%s\", session \"%s\" and step \"%s\" cannot be input "
                                    + "related or output related if the event is of type \"VALUE\"", source, session,
                            stepId);
            throw new EntityInvalidException(msg);
        }

        // Create new event
        StepPropertyUpdateRequestEvent stepPropertyEvent = new StepPropertyUpdateRequestEvent(stepId, source, session,
                                                                                              StepPropertyEventTypeEnum.VALUE,
                                                                                              stepPropertyEventInfo);
        // Publish event
        publisher.publish(stepPropertyEvent);
    }
}
