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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileReferenceWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storagelight.service.file.reference.job.FileReferenceRequestJob;
import fr.cnes.regards.modules.storagelight.service.storage.flow.StoragePluginConfigurationHandler;

/**
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileReferenceRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceRequestService.class);

    private static final int NB_REFERENCE_BY_PAGE = 1000;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IFileReferenceRequestRepository fileRefRequestRepo;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    public Optional<FileReferenceRequest> search(String destinationStorage, String checksum) {
        return fileRefRequestRepo.findByMetaInfoChecksumAndDestinationStorage(checksum, destinationStorage);
    }

    public Page<FileReferenceRequest> search(Pageable pageable) {
        return fileRefRequestRepo.findAll(pageable);
    }

    public Page<FileReferenceRequest> search(String destinationStorage, Pageable pageable) {
        return fileRefRequestRepo.findByDestinationStorage(destinationStorage, pageable);
    }

    public void deleteFileReferenceRequest(FileReferenceRequest fileRefRequest) {
        fileRefRequestRepo.deleteById(fileRefRequest.getId());
    }

    public void updateFileReferenceRequest(FileReferenceRequest fileRefRequest) {
        fileRefRequestRepo.save(fileRefRequest);
    }

    public Collection<JobInfo> scheduleStoreJobs(FileRequestStatus status, Collection<String> storages,
            Collection<String> owners) {
        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = fileRefRequestRepo.findDestinationStoragesByStatus(status);
        Set<String> storagesToSchedule = (storages != null) && !storages.isEmpty()
                ? allStorages.stream().filter(storages::contains).collect(Collectors.toSet())
                : allStorages;
        for (String storage : storagesToSchedule) {
            Page<FileReferenceRequest> filesPage;
            Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE);
            do {
                if ((owners != null) && !owners.isEmpty()) {
                    filesPage = fileRefRequestRepo.findAllByDestinationStorageAndOwnersIn(storage, owners, page);
                } else {
                    filesPage = fileRefRequestRepo.findAllByDestinationStorage(storage, page);
                }
                List<FileReferenceRequest> fileReferenceRequests = filesPage.getContent();

                if (storageHandler.getConfiguredStorages().contains(storage)) {
                    jobList = this.scheduleStoreJobsByStorage(storage, fileReferenceRequests);
                } else {
                    this.handleStorageNotAvailable(fileReferenceRequests);
                }
                page = filesPage.nextPageable();
            } while (filesPage.hasNext());
        }
        return jobList;
    }

    private Collection<JobInfo> scheduleStoreJobsByStorage(String storage,
            Collection<FileReferenceRequest> fileRefRequests) {
        Collection<JobInfo> jobInfoList = Sets.newHashSet();
        try {

            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IDataStorage storagePlugin = pluginService.getPlugin(conf.getId());

            Collection<FileReferenceWorkingSubset> workingSubSets = storagePlugin.prepareForStorage(fileRefRequests);
            workingSubSets.forEach(ws -> {
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(FileReferenceRequestJob.DATA_STORAGE_CONF_ID, conf.getId()));
                parameters.add(new JobParameter(FileReferenceRequestJob.WORKING_SUB_SET, ws));
                ws.getFileReferenceRequests().forEach(fileRefReq -> fileRefRequestRepo
                        .updateStatus(FileRequestStatus.PENDING, fileRefReq.getId()));
                jobInfoList.add(jobInfoService.createAsQueued(new JobInfo(false, 0, parameters, authResolver.getUser(),
                        FileReferenceRequestJob.class.getName())));
            });
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            this.handleStorageNotAvailable(fileRefRequests);
        }
        return jobInfoList;
    }

    public void createNewFileReferenceRequest(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            FileLocation origin, FileLocation destination) {
        this.createNewFileReferenceRequest(owners, fileMetaInfo, origin, destination, FileRequestStatus.TODO);
    }

    public void createNewFileReferenceRequest(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            FileLocation origin, FileLocation destination, FileRequestStatus status) {

        // Check if file reference request already exists
        Optional<FileReferenceRequest> oFileRefRequest = this.search(fileMetaInfo.getChecksum(),
                                                                     destination.getStorage());
        if (oFileRefRequest.isPresent()) {
            this.handleFileReferenceRequestAlreadyExists(oFileRefRequest.get(), fileMetaInfo, owners);
        } else {
            FileReferenceRequest fileRefReq = new FileReferenceRequest(owners, fileMetaInfo, origin, destination);
            fileRefReq.setStatus(status);
            if (!storageHandler.getConfiguredStorages().contains(destination.getStorage())) {
                // The storage destination is unknown, we can already set the request in error status
                String message = String
                        .format("File <%s> cannot be handle for storage as destination storage <%s> is unknown. Known storages are",
                                owners.toArray(), fileMetaInfo.getFileName(), destination.getStorage());
                fileRefReq.setStatus(FileRequestStatus.ERROR);
                fileRefReq.setErrorCause(message);
                LOGGER.error(message);
            } else {
                LOGGER.debug("New file reference request created for file <{}> to store to {} with status {}",
                             fileRefReq.getMetaInfo().getFileName(), fileRefReq.getDestination().toString(),
                             fileRefReq.getStatus());
            }
            fileRefRequestRepo.save(fileRefReq);
        }

    }

    public void handleFileReferenceRequestAlreadyExists(FileReferenceRequest fileReferenceRequest,
            FileReferenceMetaInfo newMetaInfo, Collection<String> owners) {
        boolean newOwners = false;
        for (String owner : owners) {
            if (!fileReferenceRequest.getOwners().contains(owner)) {
                fileReferenceRequest.getOwners().add(owner);
                if (newMetaInfo.equals(fileReferenceRequest.getMetaInfo())) {
                    LOGGER.warn("Existing referenced file meta information differs "
                            + "from new reference meta information. Previous ones are maintained");
                }
            }
        }
        if (newOwners) {
            fileRefRequestRepo.save(fileReferenceRequest);
        }
    }

    private void handleStorageNotAvailable(Collection<FileReferenceRequest> fileRefRequests) {
        fileRefRequests.forEach(this::handleStorageNotAvailable);
    }

    private void handleStorageNotAvailable(FileReferenceRequest fileRefReq) {
        // The storage destination is unknown, we can already set the request in error status
        fileRefReq.setStatus(FileRequestStatus.ERROR);
        fileRefReq.setErrorCause(String
                .format("File <%s> cannot be handle for storage as destination storage <%s> is unknown or disabled.",
                        fileRefReq.getMetaInfo().getFileName(), fileRefReq.getDestination().getStorage()));
    }

}
