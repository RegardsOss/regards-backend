/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.execution;

import io.vavr.collection.List;
import io.vavr.collection.Seq;

/**
 * This enum lists all the possible execution statuses, and for each one, their possible following steps,
 * making it a definition of a state machine.
 *
 * @author gandrieu
 */
public enum ExecutionStatus {

    /** Final state, ended correctly */
    SUCCESS,

    /** Final state, explicitly failed. */
    FAILURE,

    /** Final state, has been cancelled by user or admin. */
    CANCELLED,

    /** Final state, did not provide any feedback after some time. */
    TIMED_OUT,

    CLEANUP(List.of(SUCCESS)),

    /** Launched. */
    RUNNING(List.of(CLEANUP, SUCCESS, FAILURE, TIMED_OUT, CANCELLED)),

    /** Execution is being prepared (copying input files to working directory, etc.). */
    PREPARE(List.of(RUNNING)),

    /** Initial state, registered but not yet launched. */
    REGISTERED(List.of(PREPARE, RUNNING, CANCELLED)),;

    public static final ExecutionStatus[] CACHED_VALUES = values();

    private final Seq<ExecutionStatus> nextStates;

    ExecutionStatus(Seq<ExecutionStatus> nextStates) {
        this.nextStates = nextStates;
    }

    ExecutionStatus() {
        this.nextStates = List.empty();
    }

    public static Seq<ExecutionStatus> nonFinalStatusList() {
        return List.of(CACHED_VALUES).filter(s -> !s.isFinalStep());
    }

    public Seq<ExecutionStatus> getNextStates() {
        return nextStates;
    }

    public boolean isFinalStep() {
        return getNextStates().isEmpty();
    }
}
