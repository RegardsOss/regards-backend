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
package fr.cnes.regards.framework.modules.jobs.service.stub;

import java.util.List;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.service.IJobInfoService;

/**
 *
 */
@Component
public class JobInfoServiceStub implements IJobInfoService {

    @Override
    public JobInfo createJobInfo(final JobInfo pJobInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JobInfo> retrieveJobInfoList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JobInfo> retrieveJobInfoListByState(final JobStatus pState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobInfo retrieveJobInfoById(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobInfo save(final JobInfo pJobInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobInfo stopJob(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

}
