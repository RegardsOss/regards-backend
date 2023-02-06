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
package fr.cnes.regards.modules.workermanager.domain.config;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.workermanager.dto.WorkflowConfigDto;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

/**
 * Define a workflow of workers
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_workflow_config")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class WorkflowConfig {

    @Id
    @Column(name = "workflow_type")
    @Size(max = 255, message = "workflowType is limited to 255 characters")
    private String workflowType;

    /**
     * List of workers workflow
     */
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb",
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
                                    value = "fr.cnes.regards.modules.workermanager.domain.config.WorkflowStep") })
    @NotEmpty
    @Valid
    private List<WorkflowStep> steps;

    public WorkflowConfig() {
        // no-args constructor for jpa
    }

    public WorkflowConfig(String workflowType, List<WorkflowStep> steps) {
        this.workflowType = workflowType;
        this.steps = steps;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public List<WorkflowStep> getSteps() {
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
        WorkflowConfig workflowConfig = (WorkflowConfig) o;
        return workflowType.equals(workflowConfig.workflowType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowType);
    }

    @Override
    public String toString() {
        return "WorkflowConfig{" + "workflowType='" + workflowType + '\'' + ", steps=" + steps + '}';
    }

    public WorkflowConfigDto toDto() {
        return new WorkflowConfigDto(this.workflowType, this.steps.stream().map(WorkflowStep::toDto).toList());
    }
}