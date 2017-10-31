/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.chain;

import java.util.Collection;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSingleAIPGeneration;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSipValidation;

/**
 * Ingest processing service
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class IngestProcessingService implements IIngestProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingJob.class);

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IPluginService pluginService;

    @PostConstruct
    public void initDefaultPluginPackages() {
        pluginService.addPluginPackage(DefaultSingleAIPGeneration.class.getPackage().getName());
        pluginService.addPluginPackage(DefaultSipValidation.class.getPackage().getName());
    }

    @Override
    public void ingest() {
        // Retrieve all created sips
        Collection<SIPEntity> sipsToProcess = sipRepository.findAllByState(SIPState.CREATED);
        sipsToProcess.forEach(this::scheduleIngestProcessingJob);
    }

    /**
     * Schedule a new {@link IngestProcessingJob} to ingest given {@link SIPEntity}
     * @param pSipToProcess {@link SIPEntity} to ingest
     */
    private void scheduleIngestProcessingJob(SIPEntity pSipToProcess) {
        LOGGER.debug("Scheduling new IngestProcessingJob for SIP {} and processing chain {}", pSipToProcess.getId(),
                     pSipToProcess.getProcessing());
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(IngestProcessingJob.SIP_PARAMETER, pSipToProcess.getId()));
        jobParameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, pSipToProcess.getProcessing()));
        JobInfo jobInfo = new JobInfo(1, jobParameters, authResolver.getUser(), IngestProcessingJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        pSipToProcess.setState(SIPState.QUEUED);
        sipRepository.save(pSipToProcess);
    }

}
