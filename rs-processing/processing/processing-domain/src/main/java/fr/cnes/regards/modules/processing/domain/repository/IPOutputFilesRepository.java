/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain.repository;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import io.vavr.collection.List;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.UUID;

/**
 * This interface defines a repository contract for {@link POutputFile} entities.
 *
 * @author gandrieu
 */
public interface IPOutputFilesRepository {

    Flux<POutputFile> save(Flux<POutputFile> files);

    Flux<POutputFile> findByExecId(UUID execId);

    Flux<POutputFile> findByUrlIn(List<URL> urls);

    Flux<POutputFile> findByDownloadedIsTrueAndDeletedIsFalse();
}
