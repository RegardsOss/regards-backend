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

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.List;
import java.util.UUID;

/**
 * This interface defines operations on OutputFileEntities in the database.
 *
 * @author gandrieu
 */
@InstanceEntity
@Repository
public interface IOutputFileEntityRepository extends ReactiveCrudRepository<OutputFileEntity, UUID> {

    Flux<OutputFileEntity> findByExecId(UUID execId);

    Flux<OutputFileEntity> findByUrlIn(List<URL> urls);

    Flux<OutputFileEntity> findByDownloadedAndDeleted(boolean isDownloaded, boolean isDeleted);

}
