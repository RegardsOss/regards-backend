/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.dao.AIPQueryGenerator;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.RemoveAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.UpdateAIPsTagJobType;
import fr.cnes.regards.modules.storage.domain.job.UpdatedAipsInfos;
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 * Add or remove tags to several AIPs, inside a job.
 * @author Léo Mieulet
 */
public class UpdateAIPsTagJob extends AbstractJob<UpdatedAipsInfos> {

    /**
     * Job parameter name for the AIP User request id to use
     */
    public static final String FILTER_PARAMETER_NAME = "query";

    /**
     * Job parameter name for the type of update (add or remove)
     */
    public static final String UPDATE_TYPE_PARAMETER_NAME = "type";

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IAIPDao aipDao;

    /**
     * Number of created AIPs processed on each iteration by project
     */
    @Value("${regards.storage.aips.iteration.limit:100}")
    private Integer aipIterationLimit;

    @Autowired
    private INotificationClient notificationClient;

    private Map<String, JobParameter> parameters;

    private AtomicInteger nbError;

    private AtomicInteger nbEntityUpdated;

    private List<String> entityFailed;

    private Long nbEntity;

    @Override
    public void run() {
        UpdateAIPsTagJobType updateType = parameters.get(UPDATE_TYPE_PARAMETER_NAME).getValue();
        AIPQueryFilters tagFilter = parameters.get(FILTER_PARAMETER_NAME).getValue();
        Pageable pageRequest = PageRequest.of(0, aipIterationLimit, Direction.ASC, "id");
        Page<AIP> aipsPage;
        nbError = new AtomicInteger(0);
        nbEntityUpdated = new AtomicInteger(0);
        entityFailed = new ArrayList<>();
        do {
            aipsPage = aipDao.findAll(AIPQueryGenerator
                    .searchAIPContainingAllTags(tagFilter.getState(), tagFilter.getFrom(), tagFilter.getTo(),
                                                tagFilter.getTags(), tagFilter.getSession(), tagFilter.getProviderId(),
                                                tagFilter.getAipIds(), tagFilter.getAipIdsExcluded(),
                                                tagFilter.getStoredOn()),
                                      pageRequest);
            aipsPage.forEach(aip -> {
                try {
                    if (updateType == UpdateAIPsTagJobType.ADD) {
                        AddAIPTagsFilters query = (AddAIPTagsFilters) tagFilter;
                        aipService.addTags(aip, query.getTagsToAdd());
                    } else {
                        RemoveAIPTagsFilters query = (RemoveAIPTagsFilters) tagFilter;
                        aipService.removeTags(aip, query.getTagsToRemove());
                    }
                    nbEntityUpdated.incrementAndGet();
                } catch (ModuleException e) {
                    // save first 100 AIP id in error
                    if (entityFailed.size() < 100) {
                        entityFailed.add(aip.getId().toString());
                    }
                    nbError.incrementAndGet();
                    // Exception thrown while updating tag list on AIP
                    logger.error(e.getMessage(), e);
                }
            });
        } while (aipsPage.hasNext());
        nbEntity = aipsPage.getTotalElements();
        UpdatedAipsInfos infos = new UpdatedAipsInfos(nbError, nbEntityUpdated);
        this.setResult(infos);
        handleErrors(updateType);
    }

    private void handleErrors(UpdateAIPsTagJobType updateType) {
        // Handle errors
        if (nbError.get() > 0) {
            // Notify admins that the job had issues
            String title;
            if (updateType == UpdateAIPsTagJobType.ADD) {
                title = String.format("Failure while adding tag to %d AIPs", nbError.get());
            } else {
                title = String.format("Failure while removing tag to %d AIPs", nbError.get());
            }
            StringBuilder message = new StringBuilder();
            message.append(String.format("A job finished with %d successful updates and %d errors.%nAIP concerned:  ",
                                         nbEntityUpdated.get(), nbError.get()));
            for (String ipId : entityFailed) {
                message.append(ipId);
                message.append("  \\n");
            }
            FeignSecurityManager.asSystem();
            try {
                notificationClient.notify(message.toString(), title, NotificationLevel.ERROR, DefaultRole.ADMIN);
            } finally {
                FeignSecurityManager.reset();
            }
        }
    }

    @Override
    public boolean needWorkspace() {
        return false;
    }

    @Override
    public int getCompletionCount() {
        if (nbError.get() + nbEntityUpdated.get() == 0) {
            return 0;
        }
        return (int) Math.floor(100 * (nbError.get() + nbEntityUpdated.get()) / nbEntity);
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        checkParameters(parameters);
        this.parameters = parameters;
    }

    private void checkParameters(Map<String, JobParameter> parameters)
            throws JobParameterInvalidException, JobParameterMissingException {
        JobParameter typeParam = parameters.get(UPDATE_TYPE_PARAMETER_NAME);
        if (typeParam == null) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format("Job %s: parameter %s not provided", this.getClass().getName(),
                                  UPDATE_TYPE_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        if (!(typeParam.getValue() instanceof UpdateAIPsTagJobType)) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format("Job %s: cannot read the parameter %s", this.getClass().getName(),
                                  UPDATE_TYPE_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }

        JobParameter filterParam = parameters.get(FILTER_PARAMETER_NAME);
        if (filterParam == null) {

            JobParameterMissingException e = new JobParameterMissingException(String
                    .format("Job %s: parameter %s not provided", this.getClass().getName(), FILTER_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        // Check if the filterParam can be correctly parsed, depending of its type
        if (typeParam.getValue() == UpdateAIPsTagJobType.ADD && !(filterParam.getValue() instanceof AddAIPTagsFilters)
                || typeParam.getValue() == UpdateAIPsTagJobType.REMOVE
                        && !(filterParam.getValue() instanceof RemoveAIPTagsFilters)) {
            JobParameterInvalidException e = new JobParameterInvalidException(String
                    .format("Job %s: cannot read the parameter %s", this.getClass().getName(), FILTER_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

}
