/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.storage.domain.predicate;

import com.google.common.base.Objects;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;

import java.util.function.Predicate;

/**
 * Helper class providing some common predicates.
 *
 * @author Olivier Navarro
 **/
public class StoragePredicates {

    private StoragePredicates() {
    }

    public static Predicate<FileReference> fileReferenceWithSameStorageAndUrl(String storage, String url) {
        return (FileReference f) -> Objects.equal(f.getLocation().getStorage(), storage)
                                    && Objects.equal(f.getLocation().getUrl(), url);
    }

    public static Predicate<FileReference> fileReferenceWithSameStorageAndChecksum(String storage, String checksum) {
        return (FileReference f) -> Objects.equal(f.getMetaInfo().getChecksum(), checksum)
                                    && Objects.equal(f.getLocation().getStorage(), storage);
    }

    public static Predicate<FileReferenceRequestAggregation> fileReferenceRequestWithSameStorageAndChecksum(String storage,
                                                                                                            String checksum) {
        return (FileReferenceRequestAggregation req) -> Objects.equal(req.getMetaInfo().getChecksum(), checksum)
                                                        && Objects.equal(req.getStorage(), storage);
    }

    public static Predicate<FileStorageRequestAggregation> fileStorageRequestWithSameStorageAndChecksum(String storage,
                                                                                                        String checksum) {
        return (FileStorageRequestAggregation req) -> Objects.equal(req.getMetaInfo().getChecksum(), checksum)
                                                      && Objects.equal(req.getStorage(), storage);
    }

    public static Predicate<FileDeletionRequest> fileDeletionRequestWithSameStorageAndChecksum(String storage,
                                                                                               String checksum) {
        return (FileDeletionRequest req) -> Objects.equal(req.getFileReference().getMetaInfo().getChecksum(), checksum)
                                            && Objects.equal(req.getStorage(), storage);
    }
}
