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
import java.util.Set;

import org.apache.commons.compress.utils.Lists;
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
import fr.cnes.regards.modules.storagelight.dao.IFileRestorationRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileRestorationRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileRestorationWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.service.file.reference.job.FileStorageRequestJob;
import fr.cnes.regards.modules.storagelight.service.storage.flow.StoragePluginConfigurationHandler;

/**
 * Service to handle {@link FileRestorationRequest}s.
 * Those requests are created when a file reference need to be restored physically thanks to an existing {@link INearlineStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileRestorationRequestService {

    private static final int NB_REFERENCE_BY_PAGE = 500;

    @Autowired
    private IFileRestorationRequestRepository repository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    public FileRestorationRequest create(FileReference fileRefToRestore, String storage, String destinationPath) {
        return repository.save(new FileRestorationRequest(fileRefToRestore, destinationPath));
    }

    public Collection<JobInfo> scheduleRestorationJobs(FileRequestStatus status) {

        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = repository.findOriginStoragesByStatus(status);
        for (String storage : allStorages) {
            Page<FileRestorationRequest> filesPage;
            Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE);
            do {
                filesPage = repository.findAllByOriginStorage(storage, page);
                List<FileRestorationRequest> requests = filesPage.getContent();

                if (storageHandler.getConfiguredStorages().contains(storage)) {
                    jobList = this.scheduleRestorationJobsByStorage(storage, requests);
                } else {
                    this.handleStorageNotAvailable(requests);
                }
                page = filesPage.nextPageable();
            } while (filesPage.hasNext());
        }
        return jobList;
    }

    private Collection<JobInfo> scheduleRestorationJobsByStorage(String storage,
            List<FileRestorationRequest> requests) {
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
     * Update a list of {@link FileRestorationRequest}s when the storage origin cannot be handled.
     * A storage origin cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileRefRequests
     */
    private void handleStorageNotAvailable(Collection<FileRestorationRequest> fileRefRequests) {
        fileRefRequests.forEach(this::handleStorageNotAvailable);
    }

    /**
     * Update a {@link FileRestorationRequest} when the storage origin cannot be handled.
     * A storage origin cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileRefRequest
     */
    private void handleStorageNotAvailable(FileRestorationRequest request) {
        // The storage destination is unknown, we can already set the request in error status
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(String
                .format("File <%s> cannot be handle for restoration as origin storage <%s> is unknown or disabled.",
                        request.getFileReference().getMetaInfo().getFileName(), request.getOriginStorage()));
        update(request);
    }

    private FileRestorationRequest update(FileRestorationRequest request) {
        return repository.save(request);
    }

}
