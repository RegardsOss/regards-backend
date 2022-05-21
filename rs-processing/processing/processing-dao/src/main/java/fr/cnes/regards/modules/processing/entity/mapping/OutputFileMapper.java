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
package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import io.vavr.collection.List;
import org.springframework.stereotype.Component;

/**
 * This class define a mapper between domain and database entities for OutputFiles
 *
 * @author gandrieu
 */

@Component
public class OutputFileMapper implements DomainEntityMapper.OutputFile {

    @Override
    public OutputFileEntity toEntity(POutputFile domain) {
        return new OutputFileEntity(domain.getId(),
                                    domain.getExecId(),
                                    domain.getUrl(),
                                    domain.getName(),
                                    domain.getChecksum().getValue(),
                                    domain.getChecksum().getMethod(),
                                    domain.getSize(),
                                    domain.getInputCorrelationIds().asJava(),
                                    domain.getCreated(),
                                    domain.isDownloaded(),
                                    domain.isDeleted(),
                                    domain.isPersisted());
    }

    @Override
    public POutputFile toDomain(OutputFileEntity entity) {
        return new POutputFile(entity.getId(),
                               entity.getExecId(),
                               entity.getName(),
                               new POutputFile.Digest(entity.getChecksumMethod(), entity.getChecksumValue()),
                               entity.getUrl(),
                               entity.getSizeInBytes(),
                               List.ofAll(entity.getInputCorrelationIds()),
                               entity.getCreated(),
                               entity.isDownloaded(),
                               entity.isDeleted(),
                               entity.isPersisted());
    }

}
