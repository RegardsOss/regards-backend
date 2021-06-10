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
package fr.cnes.regards.framework.modules.session.agent.domain.step;

import java.util.Objects;

/**
 * Step sent to create new
 * {@link fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest}s
 *
 * @author Iliana Ghazali
 **/
public class StepProperty {

    /**
     * Step identifier
     */
    private String stepId;

    /**
     * Name of the source
     */
    private String source;

    /**
     * Name of the session
     */
    private String session;

    /**
     * Event information
     */
    private StepPropertyInfo stepPropertyInfo;

    public StepProperty(String stepId, String source, String session, StepPropertyInfo stepPropertyInfo) {
        this.stepId = stepId;
        this.source = source;
        this.session = session;
        this.stepPropertyInfo = stepPropertyInfo;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public StepPropertyInfo getStepPropertyInfo() {
        return stepPropertyInfo;
    }

    public void setStepPropertyInfo(StepPropertyInfo stepPropertyInfo) {
        this.stepPropertyInfo = stepPropertyInfo;
    }

    @Override
    public String toString() {
        return "StepProperty{" + "stepId='" + stepId + '\'' + ", source='" + source + '\'' + ", session='" + session
                + '\'' + ", stepPropertyInfo=" + stepPropertyInfo + '}';
    }
}
