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
package fr.cnes.regards.modules.acquisition.service;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.acquisition.domain.job.AcquisitionJobReport;

/**
 * {@link AcquisitionJobReport} service
 *
 * @author Marc Sordi
 *
 */
public interface IAcquisitionJobReportService {

    /**
     * Initialize a job report from related {@link JobInfo}
     * @param jobInfo related {@link JobInfo}
     * @return new {@link AcquisitionJobReport}
     */
    AcquisitionJobReport createJobReport(JobInfo jobInfo);

    /**
     * Initialize a job report from related {@link JobInfo}
     * @param jobInfo related {@link JobInfo}
     * @param session ingest session
     * @return new {@link AcquisitionJobReport}
     */
    AcquisitionJobReport createJobReport(JobInfo jobInfo, String session);

    /**
     * Update a report related to a starting job
     * @param jobReport job report to update
     */
    void reportJobStarted(AcquisitionJobReport jobReport);

    /**
     * Update a report related to a stopping job
     * @param jobReport job report to update
     */
    void reportJobStopped(AcquisitionJobReport jobReport);

    /**
     * Check if a job is stopped
     * @param jobReport job report to check
     * @return true if stopped else false
     */
    boolean isJobStopped(AcquisitionJobReport jobReport);

}
