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
package fr.cnes.regards.framework.modules.jobs.service.systemservice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.manager.JobMonitor;

/**
 * @author Léo Mieulet
 */
@Service
public class JobInfoSystemService implements IJobInfoSystemService {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JobMonitor.class);

    /**
     * {@link JobInfo} JPA Repository
     */
    private final IJobInfoRepository jobInfoRepository;

    /**
     * @param pJobInfoRepository
     *            JobInfo repository
     */
    public JobInfoSystemService(final IJobInfoRepository pJobInfoRepository) {
        super();
        jobInfoRepository = pJobInfoRepository;
    }

    @Override
    public JobInfo updateJobInfoToDone(final Long pJobInfoId, final JobStatus pJobStatus, final String pTenantName) {
        final JobInfo jobInfo = findJobInfo(pTenantName, pJobInfoId);
        if (jobInfo != null) {
            jobInfo.getStatus().setJobStatus(pJobStatus);
            jobInfo.getStatus().setStopDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));
            updateJobInfo(pTenantName, jobInfo);
        } else {
            LOG.error(String.format("Job not found %d", pJobInfoId));
        }
        return jobInfo;
    }

    @Override
    public JobInfo findJobInfo(final String pTenantId, final Long pJobInfoId) {
        return jobInfoRepository.findOne(pJobInfoId);
    }

    @Override
    public JobInfo updateJobInfo(final String pTenantId, final JobInfo pJobInfo) {
        return jobInfoRepository.save(pJobInfo);
    }

}
