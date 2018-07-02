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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.storage.dao.AIPSpecification;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.RemoveAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.UpdatedAipsInfos;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.job.UpdateAIPsTagJobType;
import fr.cnes.regards.modules.storage.service.IAIPService;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Add or remove tags to several AIPs, inside a job.
 * @author Léo Mieulet
 */
public class UpdateAIPsTagJob extends AbstractJob<UpdatedAipsInfos> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAIPsTagJob.class);

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

    private Map<String, JobParameter> parameters;

    private AtomicInteger nbError;

    private AtomicInteger nbEntityUpdated;

    private Integer nbEntity;

    @Override
    public void run() {
        UpdateAIPsTagJobType updateType = parameters.get(UPDATE_TYPE_PARAMETER_NAME).getValue();
        AIPQueryFilters tagFilter = getFilter(updateType);
        AIPSession aipSession = aipService.getSession(tagFilter.getSession(), false);
        Set<AIP> aips = aipDao.findAll(AIPSpecification.search(tagFilter.getState(), tagFilter.getFrom(), tagFilter.getTo(), tagFilter.getTags(), aipSession, tagFilter.getAipIds(), tagFilter.getAipIdsExcluded()));
        nbError = new AtomicInteger(0);
        nbEntityUpdated = new AtomicInteger(0);
        nbEntity = aips.size();
        aips.forEach(aip -> {
            try {
                if (updateType == UpdateAIPsTagJobType.ADD) {
                    AddAIPTagsFilters query = parameters.get(FILTER_PARAMETER_NAME).getValue();
                    aipService.addTags(aip, query.getTagsToAdd());
                } else {
                    RemoveAIPTagsFilters query = parameters.get(FILTER_PARAMETER_NAME).getValue();
                    aipService.removeTags(aip, query.getTagsToRemove());
                }
                nbEntityUpdated.incrementAndGet();
            } catch (ModuleException e) {
                // Exception thrown while removing tag on AIP
                LOGGER.error(e.getMessage(), e);
                nbError.incrementAndGet();
            }
        });
        UpdatedAipsInfos infos = new UpdatedAipsInfos(nbError, nbEntityUpdated);
        this.setResult(infos);
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
    public void setParameters(Map<String, JobParameter>  parameters) throws JobParameterMissingException, JobParameterInvalidException {
        checkParameters(parameters);
        this.parameters = parameters;
    }

    private void checkParameters(Map<String, JobParameter> parameters) throws JobParameterInvalidException, JobParameterMissingException {
        JobParameter typeParam = parameters.get(UPDATE_TYPE_PARAMETER_NAME);
        if (typeParam == null) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format("Job %s: parameter %s not provided", this.getClass().getName(), UPDATE_TYPE_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        if (!(typeParam.getValue() instanceof UpdateAIPsTagJobType)) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format("Job %s: cannot read the parameter %s", this.getClass().getName(), UPDATE_TYPE_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }

        JobParameter filterParam = parameters.get(FILTER_PARAMETER_NAME);
        if (filterParam == null) {

            JobParameterMissingException e = new JobParameterMissingException(
                    String.format("Job %s: parameter %s not provided", this.getClass().getName(), FILTER_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        // Check if the filterParam can be correctly parsed, depending of its type
        if ((typeParam.getValue() == UpdateAIPsTagJobType.ADD && !(filterParam.getValue() instanceof AddAIPTagsFilters)) ||
                (typeParam.getValue() == UpdateAIPsTagJobType.REMOVE && !(filterParam.getValue() instanceof RemoveAIPTagsFilters))) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format("Job %s: cannot read the parameter %s", this.getClass().getName(), FILTER_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * This job can handle add and remove tag on AIP,
     * @param updateType type of job
     * @return user query filters commonly used whenever the type of job
     */
    private AIPQueryFilters getFilter(UpdateAIPsTagJobType updateType) {
        AIPQueryFilters tagFilter = new AIPQueryFilters();
        if (updateType == UpdateAIPsTagJobType.ADD) {
            AddAIPTagsFilters query = parameters.get(FILTER_PARAMETER_NAME).getValue();
            tagFilter.setSession(query.getSession());
            tagFilter.setState(query.getState());
            tagFilter.setFrom(query.getFrom());
            tagFilter.setTo(query.getTo());
            tagFilter.setAipIds(query.getAipIds());
            tagFilter.setAipIdsExcluded(query.getAipIdsExcluded());
        } else {
            RemoveAIPTagsFilters query = parameters.get(FILTER_PARAMETER_NAME).getValue();
            tagFilter.setSession(query.getSession());
            tagFilter.setState(query.getState());
            tagFilter.setFrom(query.getFrom());
            tagFilter.setTo(query.getTo());
            tagFilter.setAipIds(query.getAipIds());
            tagFilter.setAipIdsExcluded(query.getAipIdsExcluded());
        }
        return tagFilter;
    }
}
