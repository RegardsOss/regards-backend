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
package fr.cnes.regards.framework.modules.session.manager.domain;

import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import java.time.OffsetDateTime;

/**
 * POJO used to calculated differences between two updates of {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}
 *
 * @author Iliana Ghazali
 **/
public class DeltaSessionStep {

    private StepTypeEnum type;

    private long in;

    private long out;

    private long error;

    private long waiting;

    private long running;

    private OffsetDateTime lastUpdateDate;

    private boolean sessionAdded;

    public DeltaSessionStep(StepTypeEnum type) {
        this.type = type;
        this.in = 0;
        this.out = 0;
        this.error = 0;
        this.waiting = 0;
        this.running = 0;
        this.sessionAdded = false;
        this.lastUpdateDate = null;
    }

    public StepTypeEnum getType() {
        return type;
    }

    public void setType(StepTypeEnum type) {
        this.type = type;
    }

    public long getIn() {
        return in;
    }

    public void setIn(long in) {
        this.in = in;
    }

    public long getOut() {
        return out;
    }

    public void setOut(long out) {
        this.out = out;
    }

    public long getError() {
        return error;
    }

    public void setError(long error) {
        this.error = error;
    }

    public long getWaiting() {
        return waiting;
    }

    public void setWaiting(long waiting) {
        this.waiting = waiting;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public boolean isSessionAdded() {
        return sessionAdded;
    }

    public void setSessionAdded(boolean sessionAdded) {
        this.sessionAdded = sessionAdded;
    }

    public long getRunning() {
        return running;
    }

    public void setRunning(long running) {
        this.running = running;
    }
}
