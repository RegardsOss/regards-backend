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

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionJobReportRepository;
import fr.cnes.regards.modules.acquisition.domain.job.AcquisitionJobReport;

/**
 * Acquisition job report service
 *
 * @author Marc Sordi
 *
 */
@Service
@MultitenantTransactional
public class AcquisitionJobReportService implements IAcquisitionJobReportService {

    @Autowired
    private IAcquisitionJobReportRepository jobReportRepository;

    @Override
    public AcquisitionJobReport createJobReport(JobInfo jobInfo) {
        return createJobReport(jobInfo, null);
    }

    @Override
    public AcquisitionJobReport createJobReport(JobInfo jobInfo, String session) {
        AcquisitionJobReport jobReport = new AcquisitionJobReport();
        jobReport.setScheduleDate(OffsetDateTime.now());
        jobReport.setJobId(jobInfo.getId());
        jobReport.setSession(session);
        return jobReportRepository.save(jobReport);
    }

    @Override
    public void reportJobStarted(AcquisitionJobReport jobReport) {
        jobReport.setStartDate(OffsetDateTime.now());
        jobReportRepository.save(jobReport);
    }

    @Override
    public void reportJobStopped(AcquisitionJobReport jobReport) {
        jobReport.setJobId(null);
        jobReport.setStopDate(OffsetDateTime.now());
        jobReportRepository.save(jobReport);
    }

}
