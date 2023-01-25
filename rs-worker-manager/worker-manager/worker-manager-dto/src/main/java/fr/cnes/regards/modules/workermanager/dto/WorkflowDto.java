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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

/**
 * Define a workflow of workers to execute in a specific order
 *
 * @author Iliana Ghazali
 **/
public class WorkflowDto {

    @NotBlank(message = "workflow type is mandatory")
    @Size(max = 255, message = "workflow type is limited to 255 characters")
    private final String type;
    /**
     * Ordered list of worker configurations to execute {@see WorkerConfig#workerType}
     */
    @NotEmpty(message = "at least one workerType is expected")
    private final List<String> workerTypes;

    public WorkflowDto(String type, List<String> workerTypes) {
        Assert.notNull(type, "workflow type is mandatory");
        Assert.notEmpty(workerTypes, "at least one workerType is expected");
        this.type = type;
        this.workerTypes = workerTypes;
    }

    public String getType() {
        return type;
    }

    public List<String> getWorkerTypes() {
        return workerTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkflowDto that = (WorkflowDto) o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return "WorkflowDto{" + "type='" + type + '\'' + ", workerTypes=" + workerTypes + '}';
    }
}
