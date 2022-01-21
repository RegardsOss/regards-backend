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
package fr.cnes.regards.modules.crawler.service.session;

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;

/**
 * Enumeration of notification properties in dam
 *
 * @author Iliana Ghazali
 **/
public enum SessionNotifierPropertyEnum {

    // Parameters to determine step properties
    RESET("reset", StepPropertyStateEnum.SUCCESS, true, true),

    PROPERTY_AIP_INDEXED("indexed", StepPropertyStateEnum.SUCCESS, true, true),

    PROPERTY_AIP_INDEXED_ERROR("indexedError", StepPropertyStateEnum.ERROR, false, false);

    private final String name;

    private final StepPropertyStateEnum state;

    private final boolean inputRelated;

    private final boolean outputRelated;

    SessionNotifierPropertyEnum(String name, StepPropertyStateEnum state, boolean inputRelated, boolean outputRelated) {
        this.name = name;
        this.state = state;
        this.inputRelated = inputRelated;
        this.outputRelated = outputRelated;
    }

    public String getName() {
        return name;
    }

    public StepPropertyStateEnum getState() {
        return state;
    }

    public boolean isInputRelated() {
        return inputRelated;
    }

    public boolean isOutputRelated() {
        return outputRelated;
    }

}
