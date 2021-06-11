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

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Current state of {@link SourceStepAggregation}
 *
 * @author Iliana Ghazali
 **/
@Embeddable
public class AggregationState {

    @Column(name = "errors")
    private long errors = 0L;

    @Column(name = "waiting")
    private long waiting = 0L;

    @Column(name = "running")
    private long running = 0L;

    public AggregationState() {
    }

    public AggregationState(long errors, long waiting, long running) {
        this.errors = errors;
        this.waiting = waiting;
        this.running = running;
    }

    public long getErrors() {
        return errors;
    }

    public void setErrors(long errors) {
        this.errors = errors;
    }

    public long getWaiting() {
        return waiting;
    }

    public void setWaiting(long waiting) {
        this.waiting = waiting;
    }

    public long getRunning() {
        return running;
    }

    public void setRunning(long running) {
        this.running = running;
    }

    @Override
    public String toString() {
        return "AggregationState{" + "errors=" + errors + ", waiting=" + waiting + ", running=" + running + '}';
    }
}
