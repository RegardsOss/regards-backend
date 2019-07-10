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
package fr.cnes.regards.modules.storagelight.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
import fr.cnes.regards.modules.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileReferenceRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.StorageMonitoringAggregation;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageAccessModeEnum;
import fr.cnes.regards.modules.storagelight.service.jobs.FileReferenceRequestJob;

/**
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileReferenceService {

    private static final int NB_REFERENCE_BY_PAGE = 1000;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private IFileReferenceRequestRepository fileRefRequestRepo;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    private Set<String> existingStorages;

    @PostConstruct
    public void init() {
        // Initialize existing storages
        List<PluginConfiguration> confs = pluginService.getPluginConfigurationsByType(IDataStorage.class);
        existingStorages = confs.stream().map(c -> c.getLabel()).collect(Collectors.toSet());
        // TODO : Listen for plugins modifications
    }

    /**
     * <b>Method to reference a given file</b> <br/><br />
     * If the file is <b>already referenced</b> in the destination storage,
     * this method only add the requesting owner to the file reference owner list.
     * <br/>
     * If the <b>origin destination equals the destination origin</b>, so reference the file as already stored.
     *
     * @param fileRefRequest file to reference
     * @return FileReference if already exists or does not need a new storage job
     */
    public Optional<FileReference> createFileReference(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            FileLocation origin, FileLocation destination) {

        Assert.notNull(owners, "File must have a owner to be referenced");
        Assert.isTrue(!owners.isEmpty(), "File must have a owner to be referenced");
        Assert.notNull(fileMetaInfo, "File must have an origin location to be referenced");
        Assert.notNull(origin, "File must have an origin location to be referenced");
        Assert.notNull(destination, "File must have an origin location to be referenced");

        // Does file is already referenced for the destination location ?
        Optional<FileReference> oFileRef = fileRefRepo.findByChecksumAndStorage(fileMetaInfo.getChecksum(),
                                                                                destination.getStorage());
        if (oFileRef.isPresent()) {
            this.handleFileReferenceAlreadyExists(oFileRef.get(), owners);
        } else {
            // If destination equals origin location so file is already stored and can be referenced directly
            if (destination.equals(origin)) {
                FileReference fileRef = new FileReference(owners, fileMetaInfo, origin);
                oFileRef = Optional.of(fileRefRepo.save(fileRef));
            } else {
                // Check if file reference request already exists
                Optional<FileReferenceRequest> oFileRefRequest = fileRefRequestRepo
                        .findByChecksumAndStorage(fileMetaInfo.getChecksum(), destination.getStorage());
                if (oFileRefRequest.isPresent()) {
                    this.handleFileReferenceRequestAlreadyExists(oFileRefRequest.get(), owners);
                } else {
                    this.createNewFileReferenceRequest(owners, fileMetaInfo, origin, destination);
                }
            }
        }
        return oFileRef;
    }

    public void deleteFileReferenceRequest(FileReferenceRequest fileRefRequest) {
        fileRefRequestRepo.deleteById(fileRefRequest.getId());
    }

    public void updateFileReferenceRequest(FileReferenceRequest fileRefRequest) {
        fileRefRequestRepo.save(fileRefRequest);
    }

    public Collection<StorageMonitoringAggregation> calculateTotalFileSizeAggregation(Long lastReferencedFileId) {
        if (lastReferencedFileId != null) {
            return fileRefRepo.getTotalFileSizeAggregation(lastReferencedFileId);
        } else {
            return fileRefRepo.getTotalFileSizeAggregation();
        }
    }

    public void scheduleStoreJobs() {
        Set<String> storages = fileRefRequestRepo.findStoragesByStatus(FileReferenceRequestStatus.TO_STORE);
        for (String storage : storages) {
            Page<FileReferenceRequest> filesPage;
            Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE);
            do {
                filesPage = fileRefRequestRepo.findAllByStorage(storage, page);
                if (existingStorages.contains(storage)) {
                    this.scheduleStoreJobsByStorage(storage, filesPage.getContent());
                } else {
                    this.handleStorageNotAvailable(filesPage.getContent());
                }
                page = filesPage.nextPageable();
            } while (filesPage.hasNext());
        }
    }

    private void scheduleStoreJobsByStorage(String storage, Collection<FileReferenceRequest> fileRefRequests) {
        try {
            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IDataStorage<IWorkingSubset> storagePlugin = pluginService.getPlugin(conf.getId());

            Collection<IWorkingSubset> workingSubSets = storagePlugin.prepare(fileRefRequests,
                                                                              StorageAccessModeEnum.STORE_MODE);
            workingSubSets.forEach(ws -> {
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(FileReferenceRequestJob.DATA_STORAGE_CONF_ID, conf.getId()));
                parameters.add(new JobParameter(FileReferenceRequestJob.WORKING_SUB_SET, ws));
                ws.getFileReferenceRequests().forEach(fileRefReq -> fileRefRequestRepo
                        .updateStatus(FileReferenceRequestStatus.STORE_PENDING, fileRefReq.getId()));
                jobInfoService.createAsQueued(new JobInfo(false, 0, parameters, authResolver.getUser(),
                        FileReferenceRequestJob.class.getName()));
            });
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            this.handleStorageNotAvailable(fileRefRequests);
        }
    }

    private void handleFileReferenceAlreadyExists(FileReference fileReference, Collection<String> owners) {
        boolean newOwners = false;
        for (String owner : owners) {
            if (!fileReference.getOwners().contains(owner)) {
                newOwners = true;
                fileReference.getOwners().add(owner);
                // TODO : Check if metadata information are the same. If not notify administrator.
            }
        }
        if (newOwners) {
            fileRefRepo.save(fileReference);
        }
    }

    private void handleFileReferenceRequestAlreadyExists(FileReferenceRequest fileReferenceRequest,
            Collection<String> owners) {
        boolean newOwners = false;
        for (String owner : owners) {
            if (!fileReferenceRequest.getOwners().contains(owner)) {
                fileReferenceRequest.getOwners().add(owner);
                // TODO : Check if metadata information are the same. If not notify administrator.

            }
        }
        if (newOwners) {
            fileRefRequestRepo.save(fileReferenceRequest);
        }
    }

    private void createNewFileReferenceRequest(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            FileLocation origin, FileLocation destination) {
        FileReferenceRequest fileRefReq = new FileReferenceRequest(owners, fileMetaInfo, origin, destination);
        if (!existingStorages.contains(destination.getStorage())) {
            // The storage destination is unknown, we can already set the request in error status
            fileRefReq.setStatus(FileReferenceRequestStatus.STORE_ERROR);
            fileRefReq.setErrorCause(String
                    .format("File <%> cannot be handle for storage as destination storage <%> is unknown",
                            fileMetaInfo.getFileName()));
        }
        fileRefRequestRepo.save(fileRefReq);
    }

    private void handleStorageNotAvailable(Collection<FileReferenceRequest> fileRefRequests) {
        fileRefRequests.forEach(this::handleStorageNotAvailable);
    }

    private void handleStorageNotAvailable(FileReferenceRequest fileRefReq) {
        // The storage destination is unknown, we can already set the request in error status
        fileRefReq.setStatus(FileReferenceRequestStatus.STORE_ERROR);
        fileRefReq.setErrorCause(String
                .format("File <%> cannot be handle for storage as destination storage <%> is unknown or disabled.",
                        fileRefReq.getMetaInfo().getFileName(), fileRefReq.getDestination().getStorage()));
    }

}
