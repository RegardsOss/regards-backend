/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterDescriptor;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionParameterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;
/**
 * This class defines a DTO for execution parameters.
 *
 * @author gandrieu
 */
@Data @With
@AllArgsConstructor
@Builder(toBuilder = true)

public class ExecutionParamDTO {

    private final String name;

    private final ExecutionParameterType type;

    private final String desc;

    public static ExecutionParamDTO fromProcessParam(ExecutionParameterDescriptor p) {
        return builder().desc(p.getDesc()).name(p.getName()).build();
    }
}
