/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.dto;

import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import io.vavr.collection.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.util.UUID;

/**
 * This class defines a DTO for batch requests.
 *
 * @author gandrieu
 */
@Data @With
@AllArgsConstructor
@Builder(toBuilder = true)

public class PBatchRequest {

    private final String correlationId;
    private final UUID processBusinessId;
    private final String tenant;
    private final String user;
    private final String userRole;

    private final Map<String, String> parameters;
    private final Map<String, FileSetStatistics> filesetsByDataset;

}
