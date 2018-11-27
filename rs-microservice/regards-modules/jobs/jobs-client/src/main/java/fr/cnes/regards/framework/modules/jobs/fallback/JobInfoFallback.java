/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.fallback;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.jobs.client.JobInfoClient;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobResult;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * Hystrix fallback for Feign {@link JobInfoClient}. This default implementation is executed when the circuit is open or
 * there is an error.<br>
 * To enable this fallback, set the fallback attribute to this class name in {@link JobInfoClient}.
 */
@Component
public class JobInfoFallback implements JobInfoClient {

    @Override
    public ResponseEntity<List<Resource<JobInfo>>> retrieveJobs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<Resource<JobInfo>>> retrieveJobsByState(final JobStatus pState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Resource<JobInfo>> retrieveJobInfo(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Resource<JobInfo>> stopJob(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<List<Resource<JobResult>>> getJobResults(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

}
