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
package fr.cnes.regards.modules.storagelight.service.file.reference.job;

import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileRestorationRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.IRestorationProgressManager;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;

/**
 * Implementation of {@link IStorageProgressManager} used by {@link IStorageLocation} plugins.<br/>
 * This implementation notify the system thanks to the AMQP publisher.
 *
 * @author Sébastien Binda
 */
public class FileRestorationJobProgressManager implements IRestorationProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileRestorationJobProgressManager.class);

    private final FileRefEventPublisher publisher;

    private final IJob<?> job;

    private final Set<FileRestorationRequest> handled = Sets.newHashSet();

    public FileRestorationJobProgressManager(FileRefEventPublisher publisher, IJob<?> job) {
        super();
        this.publisher = publisher;
        this.job = job;
    }

    @Override
    public void restoreSucceed(FileRestorationRequest fileReq, Path restoredFilePath) {
        FileReference fileRef = fileReq.getFileReference();
        String successMessage = String.format("File %s (checksum %s) successfully restored from %s to %s.",
                                              fileRef.getMetaInfo().getFileName(), fileRef.getMetaInfo().getChecksum(),
                                              fileRef.getLocation().toString(), restoredFilePath.toString());
        LOG.debug("[RESTORATION SUCCESS] - {}", successMessage);
        job.advanceCompletion();
        FileLocation availabilityLocation = new FileLocation(null, "file:///" + restoredFilePath.toString());
        publisher.publishFileRefAvailable(fileRef, availabilityLocation, successMessage);
        handled.add(fileReq);
    }

    @Override
    public void restoreFailed(FileRestorationRequest fileReq, String cause) {
        FileReference fileRef = fileReq.getFileReference();
        LOG.error("[RESTORATION ERROR] - Restoration error for file {} from {} (checksum: {}). Cause : {}",
                  fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                  fileRef.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        publisher.publishFileRefNotAvailable(fileRef, cause);
        handled.add(fileReq);
    }

    /**
     * Does the given {@link FileRestorationRequest} has been handled by the restorage job ?
     */
    public boolean isHandled(FileRestorationRequest req) {
        return handled.contains(req);
    }
}
