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
package fr.cnes.regards.modules.processing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.UUID;

/**
 * This class is a DTO to be used during exchange with the client.
 *
 * @author gandrieu
 */
@Value
@AllArgsConstructor(onConstructor_={@JsonCreator})
public class ProcessLabelDTO {

    UUID processBusinessId;
    String label;

    public static ProcessLabelDTO fromPluginConfiguration(PluginConfiguration pc) {
        return  new ProcessLabelDTO(UUID.fromString(pc.getBusinessId()), pc.getLabel());
    }
}
