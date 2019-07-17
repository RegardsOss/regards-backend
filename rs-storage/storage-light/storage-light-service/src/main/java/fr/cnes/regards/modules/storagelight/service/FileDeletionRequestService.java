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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
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
import fr.cnes.regards.modules.storagelight.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileDeletionWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storagelight.service.jobs.FileDeletionRequestJob;
import fr.cnes.regards.modules.storagelight.service.jobs.FileReferenceRequestJob;

/**
 * @author sbinda
 *
 */
@Service
@MultitenantTransactional
public class FileDeletionRequestService {

    private static final int NB_REFERENCE_BY_PAGE = 1000;

    @Autowired
    private IFileDeletetionRequestRepository fileDeletionRequestRepo;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    public void createNewFileDeletionRequest(FileReference fileReferenceToDelete) {
        if (!fileDeletionRequestRepo.findByFileReferenceId(fileReferenceToDelete.getId()).isPresent()) {
            fileDeletionRequestRepo.save(new FileDeletionRequest(fileReferenceToDelete));
        }
    }

    public Collection<JobInfo> scheduleDeletionJobs(FileRequestStatus status, Collection<String> storages) {

        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = fileDeletionRequestRepo.findStoragesByStatus(status);
        Set<String> deletionToSchedule = (storages != null) && !storages.isEmpty()
                ? allStorages.stream().filter(storages::contains).collect(Collectors.toSet())
                : allStorages;
        for (String storage : deletionToSchedule) {
            Page<FileDeletionRequest> deletionRequestPage;
            Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE);
            do {
                deletionRequestPage = fileDeletionRequestRepo.findByStorage(storage, page);
                if (storageHandler.getConfiguredStorages().contains(storage)) {
                    jobList = this.scheduleDeletionJobsByStorage(storage, deletionRequestPage.getContent());
                } else {
                    this.handleStorageNotAvailable(deletionRequestPage.getContent());
                }
                page = deletionRequestPage.nextPageable();
            } while (page != null);
        }
        return jobList;
    }

    private Collection<JobInfo> scheduleDeletionJobsByStorage(String storage,
            Collection<FileDeletionRequest> fileDeletionRequests) {
        Collection<JobInfo> jobInfoList = Sets.newHashSet();
        try {

            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IDataStorage storagePlugin = pluginService.getPlugin(conf.getId());

            Collection<FileDeletionWorkingSubset> workingSubSets = storagePlugin
                    .prepareForDeletion(fileDeletionRequests);
            workingSubSets.forEach(ws -> {
                Set<JobParameter> parameters = Sets.newHashSet();
                parameters.add(new JobParameter(FileReferenceRequestJob.DATA_STORAGE_CONF_ID, conf.getId()));
                parameters.add(new JobParameter(FileReferenceRequestJob.WORKING_SUB_SET, ws));
                ws.getFileDeletionRequests().forEach(fileRefReq -> fileDeletionRequestRepo
                        .updateStatus(FileRequestStatus.PENDING, fileRefReq.getId()));
                jobInfoList.add(jobInfoService.createAsQueued(new JobInfo(false, 0, parameters, authResolver.getUser(),
                        FileDeletionRequestJob.class.getName())));
            });
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            this.handleStorageNotAvailable(fileDeletionRequests);
        }
        return jobInfoList;
    }

    private void handleStorageNotAvailable(Collection<FileDeletionRequest> fileDeletionRequests) {
        fileDeletionRequests.forEach(this::handleStorageNotAvailable);
    }

    private void handleStorageNotAvailable(FileDeletionRequest fileDeletionRequest) {
        // The storage destination is unknown, we can already set the request in error status
        fileDeletionRequest.setStatus(FileRequestStatus.ERROR);
        fileDeletionRequest.setErrorCause(String
                .format("File <%s> cannot be handle for deletion as destination storage <%s> is unknown or disabled.",
                        fileDeletionRequest.getFileReference().getMetaInfo().getFileName(),
                        fileDeletionRequest.getStorage()));
    }

    /**
     * @param fileDeletionRequest
     */
    public void deleteFileDeletionRequest(FileDeletionRequest fileDeletionRequest) {
        Assert.notNull(fileDeletionRequest, "File deletion request to delete cannot be null");
        Assert.notNull(fileDeletionRequest.getId(), "File deletion request to delete identifier cannot be null");
        fileDeletionRequestRepo.deleteById(fileDeletionRequest.getId());
    }

    /**
     * @param fileDeletionRequest
     */
    public void updateFileDeletionRequest(FileDeletionRequest fileDeletionRequest) {
        Assert.notNull(fileDeletionRequest, "File deletion request to update cannot be null");
        Assert.notNull(fileDeletionRequest.getId(), "File deletion request to update identifier cannot be null");
        fileDeletionRequestRepo.save(fileDeletionRequest);
    }

}
