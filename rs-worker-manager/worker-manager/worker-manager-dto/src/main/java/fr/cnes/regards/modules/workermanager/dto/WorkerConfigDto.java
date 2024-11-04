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

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * Worker config that maps a set of content types to a worker type
 * this DTO is used by import / export microservice config
 *
 * @author Léo Mieulet
 */
public class WorkerConfigDto {

    @NotBlank(message = "Invalid worker conf with empty name")
    @Size(max = 128, message = "workerType is limited to 128 characters")
    private final String workerType;

    /**
     * Input types handled by this worker
     */
    @NotEmpty(message = "WorkerConf cannot declare empty contentTypesIn list")
    private final Set<String> contentTypeInputs;

    /**
     * Response type expected by this worker
     */
    @Nullable
    private final String contentTypeOutput;

    /**
     * Indicate if worker response in error must be stored in worker-manager database or not
     */
    private final Boolean keepErrors; // default = true

    public WorkerConfigDto(String workerType,
                           Set<String> contentTypeInputs,
                           @Nullable String contentTypeOutput,
                           boolean keepErrors) {
        Assert.notNull(workerType, "WorkerType is mandatory.");
        Assert.notNull(contentTypeInputs, "Content types is mandatory.");
        // We check later for emptiness
        this.workerType = workerType;
        this.contentTypeInputs = contentTypeInputs;
        this.contentTypeOutput = contentTypeOutput;
        this.keepErrors = keepErrors;
    }

    public String getWorkerType() {
        return workerType;
    }

    public Set<String> getContentTypeInputs() {
        return contentTypeInputs;
    }

    @Nullable
    public String getContentTypeOutput() {
        return contentTypeOutput;
    }

    public boolean isKeepErrors() {
        // if keepErrors is not specified, then it is true
        return keepErrors == null || keepErrors;
    }
}
