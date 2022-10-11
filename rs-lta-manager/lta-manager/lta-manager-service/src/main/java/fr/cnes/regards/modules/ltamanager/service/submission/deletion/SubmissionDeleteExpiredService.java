/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ltamanager.service.submission.deletion;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.settings.LtaSettings;
import fr.cnes.regards.modules.ltamanager.service.settings.LtaSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.util.Set;

import static fr.cnes.regards.modules.ltamanager.service.submission.deletion.SubmissionDeleteScheduler.DELETE_EXPIRED_REQUESTS;

/**
 * @author Iliana Ghazali
 **/
@Service
public class SubmissionDeleteExpiredService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionDeleteExpiredService.class);

    private final ISubmissionRequestRepository requestRepository;

    private final JobInfoService jobInfoService;

    private final LtaSettingService ltaSettingService;

    public SubmissionDeleteExpiredService(ISubmissionRequestRepository requestRepository,
                                          JobInfoService jobInfoService,
                                          LtaSettingService ltaSettingService) {
        this.requestRepository = requestRepository;
        this.jobInfoService = jobInfoService;
        this.ltaSettingService = ltaSettingService;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public JobInfo scheduleJob() {
        JobInfo jobInfo = null;
        OffsetDateTime expiredDate = getExpirationDate();
        LOGGER.trace("[{}] Searching for expired SubmissionRequests in success to delete before {} ...",
                     DELETE_EXPIRED_REQUESTS,
                     expiredDate);

        if (requestRepository.existsBySubmissionStatusCreationDateLessThanEqual(expiredDate)) {
            jobInfo = new JobInfo(false,
                                  0,
                                  Set.of(new JobParameter(SubmissionDeleteExpiredJob.EXPIRED_DATE, expiredDate)),
                                  null,
                                  SubmissionDeleteExpiredJob.class.getName());
            // create job
            jobInfo = jobInfoService.createAsQueued(jobInfo);
            LOGGER.trace("[{}] SubmissionRequests to delete found scheduling a SubmissionDeleteExpiredJob.",
                         DELETE_EXPIRED_REQUESTS);
        }

        return jobInfo;
    }

    private OffsetDateTime getExpirationDate() {
        Integer configuredExpirationDuration = ltaSettingService.getValue(LtaSettings.SUCCESS_EXPIRATION_IN_HOURS_KEY);
        if (configuredExpirationDuration != null) {
            return OffsetDateTime.now().minusHours(configuredExpirationDuration);
        } else {
            LOGGER.warn("The configured value {} is not present in the database. The default value {}h will be used to"
                        + " delete expired requests.",
                        LtaSettings.SUCCESS_EXPIRATION_IN_HOURS_KEY,
                        LtaSettings.DEFAULT_SUCCESS_EXPIRATION_HOURS);
            return OffsetDateTime.now().minusHours(LtaSettings.DEFAULT_SUCCESS_EXPIRATION_HOURS);
        }
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteExpiredRequests(OffsetDateTime expiredDate) {
        requestRepository.deleteByExpiredDates(expiredDate);
    }
}
