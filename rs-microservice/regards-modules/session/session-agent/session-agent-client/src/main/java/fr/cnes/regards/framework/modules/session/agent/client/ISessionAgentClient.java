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
package fr.cnes.regards.framework.modules.session.agent.client;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import java.util.List;

/**
 * Client to publish new {@link StepPropertyUpdateRequestEvent}. These events will then be used to create
 * new session steps.
 *
 * @author Iliana Ghazali
 **/
public interface ISessionAgentClient {

    /**
     * Send a {@link StepPropertyUpdateRequestEvent} with a {@link StepPropertyEventTypeEnum} of type increment. The
     * corresponding property in the session step will be incremented with the value provided.
     */
    void increment(StepProperty stepProperty);

    /**
     * Send a {@link StepPropertyUpdateRequestEvent} with a {@link StepPropertyEventTypeEnum} of type decrement. The
     * corresponding property in the session step will be decremented with the value provided.
     */
    void decrement(StepProperty stepProperty);

    /**
     * Send a {@link StepPropertyUpdateRequestEvent} with a {@link StepPropertyEventTypeEnum} of type
     * value. The corresponding properties in the session steps will be set to the value provided.
     */
    void stepValue(StepProperty stepProperty);

    /**
     * Send a bulk of {@link StepPropertyUpdateRequestEvent} with a {@link StepPropertyEventTypeEnum} of type
     * value. The corresponding properties in the session steps will be set to the value provided.
     */
    void stepValue(List<StepProperty> stepProperty);
}
