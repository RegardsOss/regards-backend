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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storagelight.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileRestorationWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.service.file.cache.CacheService;
import fr.cnes.regards.modules.storagelight.service.file.reference.job.FileStorageRequestJob;
import fr.cnes.regards.modules.storagelight.service.storage.flow.StoragePluginConfigurationHandler;

/**
 * Service to handle {@link FileCacheRequest}s.
 * Those requests are created when a file reference need to be restored physically thanks to an existing {@link INearlineStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileCacheRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheRequestService.class);

    private static final int NB_REFERENCE_BY_PAGE = 500;

    @Autowired
    private IFileCacheRequestRepository repository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    public Optional<FileCacheRequest> create(FileReference fileRefToRestore) {
        String checksum = fileRefToRestore.getMetaInfo().getChecksum();
        Optional<FileCacheRequest> oFcr = repository.findByChecksum(checksum);
        if (!oFcr.isPresent()) {
            return Optional
                    .of(repository.save(new FileCacheRequest(fileRefToRestore, cacheService.getFilePath(checksum))));
        } else {
            FileCacheRequest fcr = oFcr.get();
            if (fcr.getStatus() == FileRequestStatus.ERROR) {
                fcr.setStatus(FileRequestStatus.TODO);
                repository.save(fcr);
            }
            LOGGER.debug("File {} (checksum {}) is already requested for cache.",
                         fileRefToRestore.getMetaInfo().getFileName(), fileRefToRestore.getMetaInfo().getChecksum());
            return Optional.empty();
        }
    }

    public Collection<JobInfo> scheduleRestorationJobs(FileRequestStatus status) {

        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = repository.findStoragesByStatus(status);
        for (String storage : allStorages) {
            Page<FileCacheRequest> filesPage;
            Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE);
            do {
                filesPage = repository.findAllByStorage(storage, page);
                List<FileCacheRequest> requests = filesPage.getContent();
                if (storageHandler.getConfiguredStorages().contains(storage)) {
                    requests = calculateRestorables(requests);
                    jobList = this.scheduleRestorationJobsByStorage(storage, requests);
                } else {
                    this.handleStorageNotAvailable(requests);
                }
                page = filesPage.nextPageable();
            } while (filesPage.hasNext());
        }
        return jobList;
    }

    /**
     * Return all the request that can be restored in cache to not reach the cache size limit.
     * @param requests
     * @return available {@link FileCacheRequest} requests for restoration in cache
     */
    private List<FileCacheRequest> calculateRestorables(Collection<FileCacheRequest> requests) {
        List<FileCacheRequest> restorables = Lists.newArrayList();
        // Calculate cache size available by adding cache file sizes sum and already queued requests
        Long availableCacheSize = cacheService.getCacheAvailableSizeBytes();
        Long pendingSize = repository.getTotalFileSize();
        Long availableSize = availableCacheSize - pendingSize;
        Iterator<FileCacheRequest> it = restorables.iterator();
        Long totalSize = 0L;
        while (it.hasNext()) {
            FileCacheRequest request = it.next();
            if ((totalSize + request.getFileSize()) < availableSize) {
                restorables.add(request);
                totalSize += request.getFileSize();
            }
        }
        return restorables;
    }

    private Collection<JobInfo> scheduleRestorationJobsByStorage(String storage, List<FileCacheRequest> requests) {
        Collection<JobInfo> jobInfoList = Sets.newHashSet();
        try {
            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IStorageLocation storagePlugin = pluginService.getPlugin(conf.getId());
            Collection<FileRestorationWorkingSubset> workingSubSets = storagePlugin.prepareForRestoration(requests);
            workingSubSets.forEach(ws -> {
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(FileStorageRequestJob.DATA_STORAGE_CONF_ID, conf.getId()));
                parameters.add(new JobParameter(FileStorageRequestJob.WORKING_SUB_SET, ws));
                ws.getFileRestorationRequests()
                        .forEach(r -> repository.updateStatus(FileRequestStatus.PENDING, r.getId()));
                jobInfoList.add(jobInfoService.createAsQueued(new JobInfo(false, 0, parameters, authResolver.getUser(),
                        FileStorageRequestJob.class.getName())));
            });
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            this.handleStorageNotAvailable(requests);
        }
        return jobInfoList;
    }

    /**
     * Update a list of {@link FileCacheRequest}s when the storage origin cannot be handled.
     * A storage origin cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileRefRequests
     */
    private void handleStorageNotAvailable(Collection<FileCacheRequest> fileRefRequests) {
        fileRefRequests.forEach(this::handleStorageNotAvailable);
    }

    /**
     * Update a {@link FileCacheRequest} when the storage origin cannot be handled.
     * A storage origin cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileRefRequest
     */
    private void handleStorageNotAvailable(FileCacheRequest request) {
        // The storage destination is unknown, we can already set the request in error status
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(String
                .format("File <%s> cannot be handle for restoration as origin storage <%s> is unknown or disabled.",
                        request.getFileReference().getMetaInfo().getFileName(), request.getStorage()));
        update(request);
    }

    private FileCacheRequest update(FileCacheRequest request) {
        return repository.save(request);
    }

}
