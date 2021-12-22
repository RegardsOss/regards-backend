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
package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * This service allows to share execution results by moving them to a shared
 * folder, accessible from the caller.
 *
 * @author gandrieu
 */
public interface ISharedStorageService {

    Mono<Seq<POutputFile>> storeResult(ExecutionContext ctx, ExecutionLocalWorkdir workdir);

    POutputFile createOutputFile(UUID execId, Path storedFilePath, String outputFilename,
            List<String> inputFileCorrelationIds, long size) throws MalformedURLException;

    Path storeFile(Path filePathToCopy, UUID executionContextId) throws IOException;

    Mono<POutputFile> deleteFile(POutputFile outFile);

    Mono<POutputFile> storeOutputFile(UUID execId, Path outputFileOldPath, String outputFilename, List<String> inputFileCorrelationIds);
}
