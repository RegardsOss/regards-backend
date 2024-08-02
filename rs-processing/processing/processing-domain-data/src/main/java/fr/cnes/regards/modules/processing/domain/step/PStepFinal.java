/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.step;

import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;

import java.time.OffsetDateTime;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.toUtc;

/**
 * This class defines final steps. It must be instanciated only with a final {@link ExecutionStatus},
 * which can not be enforced by the type checker so is left to be checked in the constructor.
 *
 * @author gandrieu
 */
public class PStepFinal extends PStep {

    public PStepFinal(ExecutionStatus status, OffsetDateTime time, String message) {
        super(status, toUtc(time), message);
        if (!status.isFinalStep()) {
            throw new IllegalStateException(String.format("A final step is build with a non-final status: %s",
                                                          toString()));
        }
    }

    @Override
    public PStepFinal withTime(OffsetDateTime time) {
        return new PStepFinal(status, time, message);
    }
}
