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
package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.repository.IPOutputFilesRepository;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import fr.cnes.regards.modules.processing.entity.mapping.DomainEntityMapper;
import io.vavr.collection.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.UUID;

/**
 * This class implements is a bridge between batch domain entities and database entities.
 *
 * @author gandrieu
 */
@Component
public class POutputFileRepositoryImpl implements IPOutputFilesRepository {

    private final IOutputFileEntityRepository entityOutputFileRepo;

    private final DomainEntityMapper.OutputFile mapper;

    @Autowired
    public POutputFileRepositoryImpl(IOutputFileEntityRepository entityOutputFileRepo,
            DomainEntityMapper.OutputFile mapper) {
        this.entityOutputFileRepo = entityOutputFileRepo;
        this.mapper = mapper;
    }

    @Override
    public Flux<POutputFile> save(Flux<POutputFile> files) {
        return entityOutputFileRepo.saveAll(files.map(mapper::toEntity)).map(OutputFileEntity::persisted)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<POutputFile> findByExecId(UUID execId) {
        return entityOutputFileRepo.findByExecId(execId).map(mapper::toDomain);
    }

    @Override
    public Flux<POutputFile> findByUrlIn(List<URL> urls) {
        return entityOutputFileRepo.findByUrlIn(urls.asJava()).map(mapper::toDomain);
    }

    @Override
    public Flux<POutputFile> findByDownloadedIsTrueAndDeletedIsFalse() {
        return entityOutputFileRepo.findByDownloadedIsTrueAndDeletedIsFalse().map(mapper::toDomain);
    }
}
