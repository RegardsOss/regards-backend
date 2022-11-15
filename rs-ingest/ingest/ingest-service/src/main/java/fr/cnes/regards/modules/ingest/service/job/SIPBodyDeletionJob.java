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
package fr.cnes.regards.modules.ingest.service.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.dao.SipDeletionSchedulerRepository;
import fr.cnes.regards.modules.ingest.domain.scheduler.SipDeletionSchedulerEntity;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * This job delete the content of column rawsip of the table SIP (t_sip) when these conditions are valided:
 * <li> the last update is too old </li>
 * <li> the state is stored or deleted </li>
 * Job is optimized by avoiding delete multiple times the same sip.
 *
 * @author Thomas GUILLOU
 **/
public class SIPBodyDeletionJob extends AbstractJob<Void> {

    /**
     * The lower bound date of the sip deletion query
     * All SIP after this date should have their SIP body already deleted
     */
    public static final String LAST_SCHEDULED_DATE_PARAMETER = "lastScheduledDate";

    /**
     * The upper bound date of the sip deletion query
     */
    public static final String CLOSEST_DATE_TO_DELETE_PARAMETER = "closestDateToDelete";

    @Autowired
    private ISIPService sipService;

    @Autowired
    private SipDeletionSchedulerRepository sipDeletionSchedulerRepository;

    private OffsetDateTime lowerDateParameter;

    private OffsetDateTime upperDateParameter;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        upperDateParameter = getValue(parameters, CLOSEST_DATE_TO_DELETE_PARAMETER);
        lowerDateParameter = getValue(parameters, LAST_SCHEDULED_DATE_PARAMETER);
    }

    @Override
    public void run() {
        logger.info("[SIP DELETION SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        int numberOfSipDeleted = sipService.cleanOldRawSip(lowerDateParameter, upperDateParameter);
        updateSchedulerLastDate(upperDateParameter);
        logger.info("[SIP DELETION SCHEDULER] SIP deletion Job delete {} sip in {} ms",
                    numberOfSipDeleted,
                    System.currentTimeMillis() - start);
    }

    private void updateSchedulerLastDate(OffsetDateTime upperDate) {
        Optional<SipDeletionSchedulerEntity> optSchedulerEntity = sipDeletionSchedulerRepository.findFirst();
        SipDeletionSchedulerEntity sipDeletionSchedulerEntity = optSchedulerEntity.orElseGet(() -> new SipDeletionSchedulerEntity());
        sipDeletionSchedulerEntity.setLastScheduledDate(upperDate);
        sipDeletionSchedulerRepository.save(sipDeletionSchedulerEntity);
    }
}
