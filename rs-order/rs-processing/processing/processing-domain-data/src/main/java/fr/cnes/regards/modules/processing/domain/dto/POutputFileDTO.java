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

import fr.cnes.regards.modules.processing.domain.POutputFile;
import io.vavr.collection.List;
import lombok.Value;

import java.net.URL;
import java.util.UUID;
/**
 * This class defines a DTO for execution output files.
 *
 * @author gandrieu
 */
@Value
public class POutputFileDTO {

    UUID id;

    UUID execId;

    URL url;

    String name;

    long size;

    String checksumMethod;
    String checksumValue;

    List<String> inputCorrelationIds;

    public static POutputFileDTO toDto(POutputFile outFile) {
        return new POutputFileDTO(
            outFile.getId(),
            outFile.getExecId(),
            outFile.getUrl(),
            outFile.getName(),
            outFile.getSize(),
            outFile.getChecksum().getMethod(),
            outFile.getChecksum().getValue(),
            outFile.getInputCorrelationIds()
        );
    }

}
