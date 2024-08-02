/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.dto;

import org.springframework.util.Assert;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * @author Iliana Ghazali
 **/
public class WorkflowStepDto {

    @NotNull(message = "stepNumber must be present")
    @Min(value = 0, message = "stepNumber must be a positive number")
    private final Integer stepNumber;

    @NotBlank(message = "workerType must be present")
    @Size(max = 128, message = "workerType is limited to 128 characters")
    private final String workerType;

    public WorkflowStepDto(Integer stepNumber, String workerType) {
        Assert.isTrue(stepNumber != null && stepNumber >= 0, "stepNumber must be a valid positive number");
        Assert.notNull(workerType, "workerType is required");
        this.stepNumber = stepNumber;
        this.workerType = workerType;
    }

    public Integer getStepNumber() {
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
        WorkflowStepDto that = (WorkflowStepDto) o;
        return stepNumber.equals(that.stepNumber) && workerType.equals(that.workerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepNumber, workerType);
    }

    @Override
    public String toString() {
        return "WorkflowStepDto{" + "stepNumber=" + stepNumber + ", workerType='" + workerType + '\'' + '}';
    }
}
