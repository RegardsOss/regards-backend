/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.reference;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;
import fr.cnes.regards.modules.storagelight.service.storage.PrioritizedStorageService;

/**
 * <b>N</b>ear<b>L</b>lineFileReferenceService. Handle file reference stored on a storage location of type NEARLINE.
 * NEARLINE, means that files stored can not be retrieved synchronously. For example files stored on a  tapes.
 *
 * @author sbinda
 */
public class NLFileReferenceService {

    @Autowired
    IFileReferenceRepository fileRefRepo;

    @Autowired
    FileRefEventPublisher publisher;

    @Autowired
    FileRestorationRequestService fileRestorationRequestService;

    @Autowired
    private PrioritizedStorageService prioritizedStorageService;

    public void makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {

        Set<FileReference> availables = Sets.newHashSet();
        Set<FileReference> notAvailables = Sets.newHashSet();
        Set<FileReference> toRetrieve = Sets.newHashSet();
        Set<FileReference> refs = fileRefRepo.findByMetaInfoChecksumIn(checksums);
        Set<String> remainingChecksums = Sets.newHashSet(checksums);

        // Dispatch by storage
        ImmutableListMultimap<String, FileReference> filesByStorage = Multimaps
                .index(refs, f -> f.getLocation().getStorage());
        Set<String> remainingStorages = filesByStorage.keySet();

        Optional<PrioritizedStorage> storage = prioritizedStorageService.searchActiveHigherPriority(remainingStorages);
        // Handle storage by priority
        while (storage.isPresent() && !remainingStorages.isEmpty() && !remainingChecksums.isEmpty()) {
            // For each storage dispatch files in online, near line and not available
            PluginConfiguration conf = storage.get().getStorageConfiguration();
            String storageName = conf.getLabel();
            ImmutableList<FileReference> storageFiles = filesByStorage.get(storageName);
            switch (storage.get().getStorageType()) {
                case NEARLINE:
                    toRetrieve.addAll(storageFiles);
                    break;
                case ONLINE:
                    // If storage is an online one, so file is already available
                    availables.addAll(storageFiles);
                default:
                    // Unknown storage type
                    notAvailables.addAll(storageFiles);
                    break;
            }
            remainingChecksums.removeAll(storageFiles.stream().map(f -> f.getMetaInfo().getChecksum())
                    .collect(Collectors.toSet()));
            remainingStorages.remove(storageName);
            storage = prioritizedStorageService.searchActiveHigherPriority(remainingStorages);
        }

        notifyAvailables(availables);
        makeNearlineAvailable(toRetrieve);
    }

    private void makeNearlineAvailable(Set<FileReference> toRetrieve) {
        toRetrieve.forEach(f -> {
            fileRestorationRequestService.create(f, "cache", null);
        });
    }

    private void notifyAvailables(Set<FileReference> availables) {
        availables.forEach(f -> publisher
                .publishFileRefAvailable(f,
                                         String.format("file %s (checksum %s) available at %s",
                                                       f.getMetaInfo().getFileName(), f.getMetaInfo().getChecksum(),
                                                       f.getLocation().toString())));
    }
}
