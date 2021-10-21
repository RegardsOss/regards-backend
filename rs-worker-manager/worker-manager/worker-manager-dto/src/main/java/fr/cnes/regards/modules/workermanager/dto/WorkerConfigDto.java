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
package fr.cnes.regards.modules.workermanager.dto;
import org.springframework.util.Assert;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * Worker config that maps a set of content types to a worker type
 * this DTO is used by import / export microservice config
 * @author Léo Mieulet
 */
public class WorkerConfigDto {

    /**
     * Worker type
     */
    @NotBlank
    private String workerType;

    /**
     * List of Content Types treatable by this worker
     */
    @NotEmpty
    private Set<String> contentTypes;

    public String getWorkerType() {
        return workerType;
    }

    public Set<String> getContentTypes() {
        return contentTypes;
    }

    public void setWorkerType(String workerType) {
        this.workerType = workerType;
    }

    public void setContentTypes(Set<String> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public WorkerConfigDto(String workerType, Set<String> contentTypes) {
        Assert.notNull(workerType, "WorkerType is mandatory.");
        Assert.notNull(contentTypes, "Content types is mandatory");
        // We check later for emptiness
        this.workerType = workerType;
        this.contentTypes = contentTypes;
    }
}
