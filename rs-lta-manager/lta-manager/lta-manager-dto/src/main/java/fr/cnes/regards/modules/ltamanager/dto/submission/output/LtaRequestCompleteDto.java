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
package fr.cnes.regards.modules.ltamanager.dto.submission.output;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Give information about the status of a request when it is complete
 *
 * @author Thibaud Michaudel
 **/
public class LtaRequestCompleteDto {

    @NotBlank(message = "correlationId is required")
    @Schema(description = "correlationId of the request.")
    private final String correlationId;

    @NotNull(message = "status is required")
    @Schema(description = "Request completion status.")
    private final LtaRequestCompleteState status;

    @Nullable
    @Schema(description = "Possible error message.", nullable = true)
    private final String message;

    public LtaRequestCompleteDto(String correlationId, LtaRequestCompleteState status, String message) {
        this.correlationId = correlationId;
        this.status = status;
        this.message = message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public LtaRequestCompleteState getStatus() {
        return status;
    }

    @Nullable
    public String getMessage() {
        return message;
    }
}
