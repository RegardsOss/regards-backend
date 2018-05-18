/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.job;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;

/**
 * Implementaion of {@link IProgressManager} used by {@link IDataStorage} plugins.<br/>
 * This implementation notify the system thanks to the AMQP publisher.
 *
 * @author Sébastien Binda
 */
public class StorageJobProgressManager implements IProgressManager {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(StorageJobProgressManager.class);

    /**
     * Publisher to notify system of files events (stored, retrieved or deleted).
     */
    private final IPublisher publisher;

    /**
     * List of all failure causes throw by the {@link IDataStorage} plugins.
     */
    private final Set<String> failureCauses = Sets.newConcurrentHashSet();

    /**
     * Job associated to the current progress manager.
     */
    private final IJob<?> job;

    /**
     * Data storage configuration id which this progress manager is linked to
     */
    private final Long storageConfId;

    /**
     * Does the process is in error ?
     */
    private boolean errorStatus = false;

    /**
     * List of {@link StorageDataFile} in error during {@link IDataStorage} plugin action.
     */
    private final Collection<StorageDataFile> failedDataFile = Sets.newConcurrentHashSet();

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final String tenant;

    /**
     * Collection of data files that has been handled
     */
    private final Collection<StorageDataFile> handledDataFile = Sets.newConcurrentHashSet();

    public StorageJobProgressManager(IPublisher publisher, IJob<?> job, Long storageConfId,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.publisher = publisher;
        this.job = job;
        this.storageConfId = storageConfId;
        this.tenant = runtimeTenantResolver.getTenant();
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public void storageSucceed(StorageDataFile dataFile, URL storedUrl, Long storedFileSize) {
        LOG.debug("[STORAGE SUCCESS] - PluginConf id : {} - Store success for file {} in {} (checksum: {}).",
                  storageConfId, dataFile.getName(), storedUrl.toString(), dataFile.getChecksum());
        dataFile.setFileSize(storedFileSize);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE,
                StorageEventType.SUCCESSFULL, storageConfId, storedUrl);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        // hell yeah this is not the usual publish method, but i know what i'm doing so trust me!
        publishWithTenant(dataStorageEvent);
    }

    @Override
    public void storageFailed(StorageDataFile dataFile, Optional<URL> failedUrl, String cause) {
        LOG.error("[STORE ERROR] - PluginConf id : {} - Store error for file {} in {} (checksum: {}). Cause : {}",
                  storageConfId, dataFile.getName(), failedUrl.orElse(null), dataFile.getChecksum(), cause);
        failureCauses.add(cause);
        errorStatus = true;
        failedDataFile.add(dataFile);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.STORE, StorageEventType.FAILED,
                storageConfId, failedUrl.orElse(null));
        dataStorageEvent.setFailureCause(cause);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publishWithTenant(dataStorageEvent);
    }

    public Boolean isProcessError() {
        return errorStatus;
    }

    @Override
    public void deletionFailed(StorageDataFile dataFile, Optional<URL> failedUrl, String failureCause) {
        LOG.error("[DELETION ERROR] - PluginConf id : {} - Deletion error for file {} from {} (checksum: {}). Cause : {}",
                  storageConfId, dataFile.getName(), failedUrl.orElse(null), dataFile.getChecksum(), failureCause);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION,
                StorageEventType.FAILED, storageConfId, failedUrl.orElse(null));
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        failureCauses.add(failureCause);
        errorStatus = true;
        publishWithTenant(dataStorageEvent);
    }

    @Override
    public void deletionSucceed(StorageDataFile dataFile, URL deletedUrl) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.DELETION,
                StorageEventType.SUCCESSFULL, storageConfId, deletedUrl);
        LOG.debug("[DELETION SUCCESS] - PluginConf id : {} - Deletion success for file {} from {} (checksum: {})",
                  storageConfId, dataFile.getName(), deletedUrl.toString(), dataFile.getChecksum());
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publishWithTenant(dataStorageEvent);
    }

    @Override
    public void restoreSucceed(StorageDataFile dataFile, URL restoredFromUrl, Path restoredFilePath) {
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.RESTORATION,
                StorageEventType.SUCCESSFULL, storageConfId, restoredFromUrl);
        LOG.debug("[RESTORATION SUCCESS] - PluginConf id : {} - Restoration success for file {} from {} (checksum: {})",
                  storageConfId, dataFile.getName(), restoredFromUrl.toString(), dataFile.getChecksum());
        dataStorageEvent.setRestorationPath(restoredFilePath);
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publishWithTenant(dataStorageEvent);
    }

    @Override
    public void restoreFailed(StorageDataFile dataFile, Optional<URL> failedUrl, String failureCause) {
        LOG.error("[RESTORATION ERROR] - PluginConf id : {} - Restoration error for file {} from {} (checksum: {}). Cause : {}",
                  storageConfId, dataFile.getName(), failedUrl.orElse(null), dataFile.getChecksum(), failureCause);
        DataStorageEvent dataStorageEvent = new DataStorageEvent(dataFile, StorageAction.RESTORATION,
                StorageEventType.FAILED, storageConfId, failedUrl.orElse(null));
        failureCauses.add(failureCause);
        errorStatus = true;
        handledDataFile.add(dataFile);
        job.advanceCompletion();
        publishWithTenant(dataStorageEvent);
    }

    private void publishWithTenant(DataStorageEvent event) {
        runtimeTenantResolver.forceTenant(tenant);
        publisher.publish(event);
        runtimeTenantResolver.clearTenant();
    }

    /**
     * @return the failure causes
     */
    public Set<String> getFailureCauses() {
        return failureCauses;
    }

    /**
     * @return the failed data files
     */
    public Collection<StorageDataFile> getFailedDataFile() {
        return failedDataFile;
    }

    /**
     * @return the handled data files
     */
    public Collection<StorageDataFile> getHandledDataFile() {
        return handledDataFile;
    }
}
