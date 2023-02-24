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

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * @author Iliana Ghazali
 **/
public class SubmissionDeleteExpiredJob extends AbstractJob<Void> {

    public static final String EXPIRED_DATE = "EXPIRED_DATE";

    private OffsetDateTime expiredDate;

    @Autowired
    private SubmissionDeleteExpiredService deleteExpiredService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        this.expiredDate = getValue(parameters, EXPIRED_DATE, new TypeToken<OffsetDateTime>() {

        }.getType());
    }

    @Override
    public void run() {
        logger.debug("[{}] SubmissionDeleteExpiredJob starts", jobInfoId);
        long start = System.currentTimeMillis();
        deleteExpiredService.deleteExpiredRequests(expiredDate);
        logger.debug("[{}] SubmissionDeleteExpiredJob ended in {}ms.", jobInfoId, System.currentTimeMillis() - start);
    }

}