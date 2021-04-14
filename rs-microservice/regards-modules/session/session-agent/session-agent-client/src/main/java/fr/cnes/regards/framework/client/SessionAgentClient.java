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
package fr.cnes.regards.framework.client;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.domain.EventTypeEnum;
import fr.cnes.regards.framework.domain.StepEvent;
import fr.cnes.regards.framework.domain.StepEventStateEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TODO Description
 *
 * @author TODO
 */
@Component
public class SessionAgentClient {

    @Autowired
    private IPublisher publisher;


    public void increment(String stepId, String source, String session, StepTypeEnum stepType, String property,
            String value,
                          StepEventStateEnum state, boolean input_related, boolean output_related) {
        // Create new event
        StepEvent stepEvent = new StepEvent(stepId, source, session, stepType, state, property, value,
                EventTypeEnum.INC, input_related, output_related);
        // Publish event
        publisher.publish(stepEvent);
    }

    public void decrement(String stepId, String source, String session, StepTypeEnum stepType, String property,
            String value,
                          StepEventStateEnum state, boolean input_related, boolean output_related) {
        // Create new event
        StepEvent stepEvent = new StepEvent(stepId, source, session, stepType, state, property, value,
                EventTypeEnum.DEC, input_related, output_related);
        // Publish event
        publisher.publish(stepEvent);
    }

    public void stepValue(String stepId, String source, String session, StepTypeEnum stepType, String property,
            String value, StepEventStateEnum state, boolean input_related, boolean output_related) {
        // Create new event
        StepEvent stepEvent = new StepEvent(stepId, source, session, stepType, state, property, value,
                EventTypeEnum.VALUE, input_related, output_related);
        // Publish event
        publisher.publish(stepEvent);
    }


}
