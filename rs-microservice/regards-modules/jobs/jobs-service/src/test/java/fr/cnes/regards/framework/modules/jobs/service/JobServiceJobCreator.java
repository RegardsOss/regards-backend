/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.service;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.WaiterJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Léo Mieulet
 */
@Component
public class JobServiceJobCreator {

    @Autowired
    private IJobInfoService jobInfoService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobInfo[] runWaitJobs() {
        JobInfo[] jobInfos = new JobInfo[6];
        for (int i = 0; i < jobInfos.length; i++) {
            jobInfos[i] = createWaitJob(1000L, 2, 20 - i); // makes it easier to know which ones are launched first
        }
        for (int i = 0; i < jobInfos.length; i++) {
            jobInfos[i] = jobInfoService.createAsQueued(jobInfos[i]);
        }
        return jobInfos;
    }

    public JobInfo createWaitJob(long waitPeriod, int waitPeriodCount, int jobPriority) {
        JobInfo waitJobInfo = new JobInfo(false);
        waitJobInfo.setPriority(jobPriority);
        waitJobInfo.setClassName(WaiterJob.class.getName());
        waitJobInfo.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, waitPeriod),
                                  new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, waitPeriodCount));
        return waitJobInfo;
    }
}
