/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.domain.config;

import fr.cnes.regards.modules.workermanager.dto.WorkflowStepDto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;

/**
 * Step of a {@link WorkflowConfig}
 *
 * @author Iliana Ghazali
 **/
public class WorkflowStep {

    @NotNull(message = "stepNumber must be present")
    @Min(value = 0, message = "stepNumber must be a positive number")
    private final int stepNumber;

    @NotBlank(message = "workerType must be present")
    @Size(max = 128, message = "workerType is limited to 128 characters")
    private final String workerType;

    public WorkflowStep(int stepNumber, String workerType) {
        this.stepNumber = stepNumber;
        this.workerType = workerType;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getWorkerType() {
        return workerType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkflowStep that = (WorkflowStep) o;
        return stepNumber == that.stepNumber && workerType.equals(that.workerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepNumber, workerType);
    }

    @Override
    public String toString() {
        return "WorkflowStep{" + "stepNumber=" + stepNumber + ", workerType='" + workerType + '\'' + '}';
    }

    public WorkflowStepDto toDto() {
        return new WorkflowStepDto(stepNumber, workerType);
    }
}
