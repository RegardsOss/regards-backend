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

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileReferenceRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
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
public class FileReferenceRequestService implements IHandler<PluginConfEvent> {

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

    private Set<String> existingStorages;

    @Autowired
    private ISubscriber subscriber;

    @PostConstruct
    public void init() {
        // Initialize existing storages
        List<PluginConfiguration> confs = pluginService.getPluginConfigurationsByType(IDataStorage.class);
        existingStorages = confs.stream().map(c -> c.getLabel()).collect(Collectors.toSet());
        // Listen for plugins modifications
        subscriber.subscribeTo(PluginConfEvent.class, this);
    }

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

    public Collection<JobInfo> scheduleStoreJobs(FileReferenceRequestStatus status, Collection<String> storages,
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
                if (existingStorages.contains(storage)) {
                    jobList = this.scheduleStoreJobsByStorage(storage, filesPage.getContent());
                } else {
                    this.handleStorageNotAvailable(filesPage.getContent());
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
            IDataStorage<IWorkingSubset> storagePlugin = pluginService.getPlugin(conf.getId());

            Collection<IWorkingSubset> workingSubSets = storagePlugin.prepare(fileRefRequests,
                                                                              StorageAccessModeEnum.STORE_MODE);
            workingSubSets.forEach(ws -> {
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(FileReferenceRequestJob.DATA_STORAGE_CONF_ID, conf.getId()));
                parameters.add(new JobParameter(FileReferenceRequestJob.WORKING_SUB_SET, ws));
                ws.getFileReferenceRequests().forEach(fileRefReq -> fileRefRequestRepo
                        .updateStatus(FileReferenceRequestStatus.STORE_PENDING, fileRefReq.getId()));
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
        FileReferenceRequest fileRefReq = new FileReferenceRequest(owners, fileMetaInfo, origin, destination);
        if (!existingStorages.contains(destination.getStorage())) {
            // The storage destination is unknown, we can already set the request in error status
            String message = String
                    .format("File <%s> cannot be handle for storage as destination storage <%s> is unknown",
                            fileMetaInfo.getFileName(), destination.getStorage());
            fileRefReq.setStatus(FileReferenceRequestStatus.STORE_ERROR);
            fileRefReq.setErrorCause(message);
            LOGGER.error(message);
        } else {
            LOGGER.debug("New file reference request created for file <{}> to store to {} with status {}",
                         fileRefReq.getMetaInfo().getFileName(), fileRefReq.getDestination().toString(),
                         fileRefReq.getStatus());
        }
        fileRefRequestRepo.save(fileRefReq);

    }

    public void handleFileReferenceRequestAlreadyExists(FileReferenceRequest fileReferenceRequest,
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

    private void handleStorageNotAvailable(Collection<FileReferenceRequest> fileRefRequests) {
        fileRefRequests.forEach(this::handleStorageNotAvailable);
    }

    private void handleStorageNotAvailable(FileReferenceRequest fileRefReq) {
        // The storage destination is unknown, we can already set the request in error status
        fileRefReq.setStatus(FileReferenceRequestStatus.STORE_ERROR);
        fileRefReq.setErrorCause(String
                .format("File <%s> cannot be handle for storage as destination storage <%s> is unknown or disabled.",
                        fileRefReq.getMetaInfo().getFileName(), fileRefReq.getDestination().getStorage()));
    }

    @Override
    public void handle(TenantWrapper<PluginConfEvent> wrapper) {
        if ((wrapper.getContent().getPluginTypes().contains(IDataStorage.class.getName()))) {
            try {
                switch (wrapper.getContent().getAction()) {
                    case CREATE:
                        existingStorages.add(pluginService
                                .getPluginConfiguration(wrapper.getContent().getPluginConfId()).getLabel());
                        break;
                    case DELETE:
                        existingStorages.remove(pluginService
                                .getPluginConfiguration(wrapper.getContent().getPluginConfId()).getLabel());
                        break;
                    case ACTIVATE:
                    case DISABLE:
                    case UPDATE:
                    default:
                        break;
                }
            } catch (EntityNotFoundException e) {
                // Nothing to do, message is not valid.
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
