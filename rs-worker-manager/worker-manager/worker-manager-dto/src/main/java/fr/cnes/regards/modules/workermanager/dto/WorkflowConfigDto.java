/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

/**
 * Define a workflow of workers to execute in a specific order
 *
 * @author Iliana Ghazali
 **/
public class WorkflowConfigDto {

    @NotBlank(message = "workflowType is mandatory")
    @Size(max = 255, message = "workflowType is limited to 255 characters")
    private final String workflowType;

    /**
     * Ordered list of worker configurations to execute {@see WorkerConfig#workerType}
     */
    @NotEmpty(message = "at least one step is expected")
    @Valid
    private final List<WorkflowStepDto> steps;

    public WorkflowConfigDto(String workflowType, List<WorkflowStepDto> steps) {
        Assert.notNull(workflowType, "workflowType is mandatory");
        Assert.notEmpty(steps, "at least one step is expected");
        this.workflowType = workflowType;
        this.steps = steps;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public List<WorkflowStepDto> getSteps() {
        return steps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkflowConfigDto that = (WorkflowConfigDto) o;
        return workflowType.equals(that.workflowType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowType);
    }

    @Override
    public String toString() {
        return "WorkflowConfigDto{" + "workflowType='" + workflowType + '\'' + ", steps=" + steps + '}';
    }
}
