/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.hibernate.Hibernate;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class TestJobInfoService implements ITestJobInfoService {

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Override
    public JobInfo findHighestPriorityQueuedJobAndSetAsToBeRun() {
        JobInfo found = jobInfoRepository.findHighestPriorityQueued();
        LoggerFactory.getLogger("TEST_JOB_SERVICE").error("------------------\nFound Job " + found.toString());
        if (found != null) {
            Hibernate.initialize(found.getParameters());
            found.updateStatus(JobStatus.TO_BE_RUN);
            jobInfoRepository.save(found);
        }
        return found;
    }
}
