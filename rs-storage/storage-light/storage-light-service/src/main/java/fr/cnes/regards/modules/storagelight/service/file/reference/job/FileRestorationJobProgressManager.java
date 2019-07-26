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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IRestorationProgressManager;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageProgressManager;

/**
 * Implementation of {@link IStorageProgressManager} used by {@link IStorageLocation} plugins.<br/>
 * This implementation notify the system thanks to the AMQP publisher.
 *
 * @author Sébastien Binda
 */
public class FileRestorationJobProgressManager implements IRestorationProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileRestorationJobProgressManager.class);

    private final IPublisher publisher;

    private final IJob<?> job;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final String tenant;

    public FileRestorationJobProgressManager(IPublisher publisher, IJob<?> job,
            IRuntimeTenantResolver runtimeTenantResolver, String tenant) {
        super();
        this.publisher = publisher;
        this.job = job;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenant = tenant;
    }

    @Override
    public void restoreSucceed(FileReference fileRef, Path restoredFilePath) {
        String successMessage = String.format("File %s successfully restored from %s to %s (checksum : %s).",
                                              fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                                              restoredFilePath.toString(), fileRef.getMetaInfo().getChecksum());
        LOG.debug("[RESTORATION SUCCESS] - {}", successMessage);
        job.advanceCompletion();
        FileReferenceEvent event = new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.AVAILABLE, fileRef.getOwners(), successMessage,
                new FileLocation(null, "file:///" + restoredFilePath.toString()));
        publishWithTenant(event);
    }

    @Override
    public void restoreFailed(FileReference fileRef, String cause) {
        LOG.error("[RESTORATION ERROR] - Restoration error for file {} from {} (checksum: {}). Cause : {}",
                  fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                  fileRef.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        FileReferenceEvent event = new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.AVAILABILITY_ERROR, fileRef.getOwners(), cause);
        publishWithTenant(event);
    }

    private void publishWithTenant(FileReferenceEvent event) {
        runtimeTenantResolver.forceTenant(tenant);
        publisher.publish(event);
    }
}
