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

import fr.cnes.regards.modules.workermanager.dto.WorkflowDto;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;
import java.util.Objects;

/**
 * Define a workflow of workers
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_workflow")
public class Workflow {

    public static final String WORKFLOW_TYPE_NAME = "type";

    public static final String WORKER_TYPES_NAME = "worker_types";

    @Id
    @Column(name = WORKFLOW_TYPE_NAME)
    private String type;

    /**
     * List of workers workflow
     */
    @Column(columnDefinition = "jsonb", name = WORKER_TYPES_NAME)
    @Type(type = "jsonb")
    private List<String> workerTypes;

    public Workflow() {
        // no-args constructor for jpa
    }

    public Workflow(String type, List<String> workerTypes) {
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
        Workflow workflow = (Workflow) o;
        return type.equals(workflow.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return "Workflow{" + "type='" + type + '\'' + ", workerConfigs=" + workerTypes + '}';
    }

    public WorkflowDto toDto() {
        return new WorkflowDto(this.type, this.workerTypes);
    }
}
