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
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ISessionAgentClient}
 *
 * @author Iliana Ghazali
 */
@Service
public class SessionAgentClient implements ISessionAgentClient {

    /**
     * Publisher to publish {@link StepPropertyUpdateRequestEvent}
     */
    @Autowired
    private IPublisher publisher;

    @Override
    public void increment(StepProperty stepProperty) {
        // Create new event
        StepPropertyUpdateRequestEvent stepPropertyEvent = new StepPropertyUpdateRequestEvent(stepProperty,
                                                                                              StepPropertyEventTypeEnum.INC);
        // Publish event
        publisher.publish(stepPropertyEvent);
    }


    @Override
    public void decrement(StepProperty stepProperty) {
        // Create new event
        StepPropertyUpdateRequestEvent stepPropertyEvent = new StepPropertyUpdateRequestEvent(stepProperty,
                                                                                              StepPropertyEventTypeEnum.DEC);
        // Publish event
        publisher.publish(stepPropertyEvent);
    }


    @Override
    public void stepValue(StepProperty stepProperty) throws EntityInvalidException {
        // check if input and output are false
        if (stepProperty.getStepPropertyInfo().isInputRelated() || stepProperty.getStepPropertyInfo()
                .isOutputRelated()) {
            String msg = String
                    .format("Step property with source \"%s\", session \"%s\" and step \"%s\" cannot be input "
                                    + "related or output related if the event is of type \"VALUE\"",
                            stepProperty.getSource(), stepProperty.getSession(), stepProperty.getStepId());
            throw new EntityInvalidException(msg);
        }

        // Create new event
        StepPropertyUpdateRequestEvent stepPropertyEvent = new StepPropertyUpdateRequestEvent(stepProperty,
                                                                                              StepPropertyEventTypeEnum.VALUE);
        // Publish event
        publisher.publish(stepPropertyEvent);
    }
}