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
package fr.cnes.regards.framework.modules.session.agent.domain.step;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;

/**
 * Event states for {@link StepPropertyUpdateRequestEvent}. These parameters can modify the state and the
 * input/output values of the {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}
 *
 * @author Iliana Ghazali
 **/
public enum StepPropertyStateEnum {
    /**
     * When the event in success and is related to an input and/or an output
     */
    SUCCESS,
    /**
     * When the event sent has an impact on the running state of the SessionStep
     */
    RUNNING,
    /**
     * When the event sent has an impact on the error state of the SessionStep
     */
    ERROR,
    /**
     * When the event sent has an impact on the waiting state of the SessionStep
     */
    WAITING,
    /**
     * When the event is given for information. It has no impact on the state of the step.
     */
    INFO;
}