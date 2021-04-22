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
package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.step.PStepFinal;
import fr.cnes.regards.modules.processing.domain.step.PStepIntermediary;
import io.vavr.collection.List;
import lombok.Data;

import java.time.OffsetDateTime;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;


/**
 * This class defines an abstract step. Subclasses are {@link PStepFinal} and {@link PStepIntermediary}.
 *
 * A step is immutable.
 *
 * @author gandrieu
 */
@Data
public abstract class PStep {

    protected final ExecutionStatus status;
    protected final OffsetDateTime time;
    protected final String message;

    public abstract PStep withTime(OffsetDateTime time);

    public static PStep from(ExecutionStatus status, OffsetDateTime time, String message) {
        return status.isFinalStep()
                ? new PStepFinal(status, time, message)
                : new PStepIntermediary(status, time, message);
    }

    public static PStepIntermediary registered(String message) {
        return new PStepIntermediary(REGISTERED, nowUtc(), message);
    }

    public static PStepIntermediary prepare(String message) {
        return new PStepIntermediary(PREPARE, nowUtc(), message);
    }

    public static PStepIntermediary running(String message) {
        return new PStepIntermediary(RUNNING, nowUtc(), message);
    }

    public static PStepIntermediary cleanup(String message) {
        return new PStepIntermediary(CLEANUP, nowUtc(), message);
    }

    public static PStepFinal cancelled(String message) {
        return new PStepFinal(CANCELLED, nowUtc(), message);
    }

    public static PStepFinal timeout(String message) {
        return new PStepFinal(TIMED_OUT, nowUtc(), message);
    }

    public static PStepFinal success(String message) {
        return new PStepFinal(SUCCESS, nowUtc(), message);
    }

    public static PStepFinal failure(String message) {
        return new PStepFinal(FAILURE, nowUtc(), message);
    }

    public static PStepSequence sequence(PStep... steps) {
        return new PStepSequence(List.of(steps));
    }

    public PStep clean() { return from(status, time, message); }
}
