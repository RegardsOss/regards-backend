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
package fr.cnes.regards.framework.modules.session.commons.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Iliana Ghazali
 **/
public class SessionStepId implements Serializable {


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

    public SessionStepId(String stepId, String source, String session) {
        this.stepId = stepId;
        this.source = source;
        this.session = session;
    }

    public SessionStepId(){
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SessionStepId that = (SessionStepId) o;
        return stepId.equals(that.stepId) && source.equals(that.source) && session.equals(that.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepId, source, session);
    }
}
