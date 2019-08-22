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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

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
import fr.cnes.regards.modules.storage.domain.job.RemovedAipsInfos;
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 * JOB to delete multiple AIPs. AIPs to delete are defined with the search query parameter named "query".
 * @author Léo Mieulet
 */
public class DeleteAIPsJob extends AbstractJob<RemovedAipsInfos> {

    /**
     * Job parameter name for the AIP User request id to use
     */
    public static final String FILTER_PARAMETER_NAME = "query";

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IAIPDao aipDao;

    @Autowired
    private INotificationClient notificationClient;

    /**
     * Limit number of AIPs to delete in one job.
     */
    @Value("${regards.storage.aips.iteration.limit:100}")
    private Integer aipIterationLimit;

    private Map<String, JobParameter> parameters;

    private AtomicInteger nbError;

    private AtomicInteger nbEntityRemoved;

    private Long nbEntity;

    private ArrayList<String> entityFailed;

    @Override
    public void run() {
        AIPQueryFilters tagFilter = parameters.get(FILTER_PARAMETER_NAME).getValue();
        Pageable pageRequest = PageRequest.of(0, aipIterationLimit, Direction.ASC, "id");
        Page<AIP> aipsPage;
        nbError = new AtomicInteger(0);
        nbEntityRemoved = new AtomicInteger(0);
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
                    aipService.deleteAip(aip);
                    nbEntityRemoved.incrementAndGet();
                } catch (ModuleException e) {
                    // save first 100 AIP id in error
                    if (entityFailed.size() < 100) {
                        entityFailed.add(aip.getId().toString());
                    }
                    // Exception thrown while removing AIP
                    LOGGER.error(e.getMessage(), e);
                    nbError.incrementAndGet();
                }
            });
            pageRequest = aipsPage.nextPageable();
        } while (aipsPage.hasNext());
        nbEntity = aipsPage.getTotalElements();
        RemovedAipsInfos infos = new RemovedAipsInfos(nbError, nbEntityRemoved);
        this.setResult(infos);
        handleErrors();
    }

    private void handleErrors() {
        // Handle errors
        if (nbError.get() > 0) {
            // Notify admins that the job had issues
            String title = String.format("Failure while removing %d AIPs", nbError.get());
            StringBuilder message = new StringBuilder();
            message.append(String
                    .format("A job finished with %d AIP correctly removed and %d errors.%nAIP concerned:  ",
                            nbEntityRemoved.get(), nbError.get()));
            for (String ipId : entityFailed) {
                message.append(ipId);
                message.append("  \\n");
            }
            notificationClient.notify(message.toString(), title, NotificationLevel.ERROR, DefaultRole.ADMIN);
        }
    }

    @Override
    public boolean needWorkspace() {
        return false;
    }

    @Override
    public int getCompletionCount() {
        if ((nbError.get() + nbEntityRemoved.get()) == 0) {
            return 0;
        }
        return (int) Math.floor((100 * (nbError.get() + nbEntityRemoved.get())) / nbEntity);
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        checkParameters(parameters);
        this.parameters = parameters;
    }

    private void checkParameters(Map<String, JobParameter> parameters)
            throws JobParameterInvalidException, JobParameterMissingException {
        JobParameter filterParam = parameters.get(FILTER_PARAMETER_NAME);
        if (filterParam == null) {

            JobParameterMissingException e = new JobParameterMissingException(String
                    .format("Job %s: parameter %s not provided", this.getClass().getName(), FILTER_PARAMETER_NAME));
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
        // Check if the filterParam can be correctly parsed, depending of its type
        if (!(filterParam.getValue() instanceof AIPQueryFilters)) {
            JobParameterInvalidException e = new JobParameterInvalidException(String
                    .format("Job %s: cannot read the parameter %s", this.getClass().getName(), FILTER_PARAMETER_NAME));
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }
}
