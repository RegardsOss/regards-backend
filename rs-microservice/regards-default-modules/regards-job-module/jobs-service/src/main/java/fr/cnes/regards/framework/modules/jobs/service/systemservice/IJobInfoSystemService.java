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

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * @author Léo Mieulet
 */
public interface IJobInfoSystemService {

    /**
     * @param pTenantName
     *            the tenant name
     * @param pJobInfoId
     *            the jobInfo id
     * @return the jobInfo
     */
    JobInfo findJobInfo(final String pTenantName, final Long pJobInfoId);

    /**
     * @param pTenantId
     *            the tenant name
     * @param pJobInfo
     *            the jobInfo id
     * @return the updated jobInfo
     */
    JobInfo updateJobInfo(String pTenantId, JobInfo pJobInfo);

    /**
     * Setup the end date
     *
     * @param pJobInfoId
     *            the jobInfo id
     * @param pJobStatus
     *            the new jobStatus (succeeded, failed)
     * @param pTenantName
     *            the tenant name
     * @return the updated jobInfo
     */
    JobInfo updateJobInfoToDone(final Long pJobInfoId, final JobStatus pJobStatus, final String pTenantName);
}
