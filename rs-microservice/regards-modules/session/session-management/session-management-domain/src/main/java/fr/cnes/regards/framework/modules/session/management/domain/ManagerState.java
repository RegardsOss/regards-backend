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
package fr.cnes.regards.framework.modules.session.management.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 * Common entity to handle {@link Session} or {@link Source} states
 *
 * @author Iliana Ghazali
 **/
@Embeddable
public class ManagerState {

    /**
     * If Source or Session is running, ie, if one of Source or Session is running
     */
    @Column(name = "running")
    @NotNull
    private boolean running = false;

    /**
     * If Source or Session is in error, ie, if one of Source or Session is in error state
     */
    @Column(name = "errors")
    @NotNull
    private boolean errors = false;

    /**
     * If Source or Session is waiting, ie, if one of Source or Session is in waiting state
     */
    @Column(name = "waiting")
    @NotNull
    private boolean waiting = false;

    public ManagerState() {
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isErrors() {
        return errors;
    }

    public void setErrors(boolean errors) {
        this.errors = errors;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }
}
