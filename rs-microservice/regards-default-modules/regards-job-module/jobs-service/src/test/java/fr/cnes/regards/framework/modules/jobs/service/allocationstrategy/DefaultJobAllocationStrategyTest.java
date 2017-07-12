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
package fr.cnes.regards.framework.modules.jobs.service.allocationstrategy;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import fr.cnes.regards.framework.modules.jobs.service.allocationstrategy.DefaultJobAllocationStrategy;
import fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy.IJobQueue;
import fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy.JobAllocationStrategyResponse;
import fr.cnes.regards.framework.modules.jobs.service.crossmoduleallocationstrategy.JobQueue;

/**
 * @author Léo Mieulet
 */
public class DefaultJobAllocationStrategyTest {

    @Test
    public void testInitialisation() {
        final IJobAllocationStrategy defaultJobAllocationStrategy = new DefaultJobAllocationStrategy();
        final List<String> pProjects = new ArrayList<>();
        final int nbProjects = 9;
        for (int i = 0; i < nbProjects; i++) {
            pProjects.add(String.format("project%d", i));
        }
        final JobAllocationStrategyResponse jobAllocationStrategyResponse = defaultJobAllocationStrategy
                .getNextQueue(pProjects, null, 100);
        Assertions.assertThat(jobAllocationStrategyResponse.getJobQueueList().size()).isEqualByComparingTo(nbProjects);
        Assertions.assertThat(jobAllocationStrategyResponse.getJobQueueList().get(0).getMaxSize())
                .isEqualByComparingTo(12);
    }

    @Test
    public void testScaleWhenNewProjects() {
        final IJobAllocationStrategy defaultJobAllocationStrategy = new DefaultJobAllocationStrategy();
        final List<String> projects = new ArrayList<>();
        final List<IJobQueue> jobQueueList = new ArrayList<>();

        final int nbPreviousQueues = 4;
        final int nbThreadForPreviousProject = 7;
        for (int i = 0; i < nbPreviousQueues; i++) {
            jobQueueList.add(new JobQueue(String.format("project%d", i), nbThreadForPreviousProject, 25));
        }

        final int nbProjects = 1000;
        for (int i = 0; i < nbProjects; i++) {
            projects.add(String.format("project%d", i));
        }
        Assertions.assertThat(jobQueueList.size()).isNotEqualTo(projects.size());
        final JobAllocationStrategyResponse jobAllocationStrategyResponse = defaultJobAllocationStrategy
                .getNextQueue(projects, jobQueueList, 100);
        Assertions.assertThat(jobAllocationStrategyResponse.getJobQueueList().size()).isEqualByComparingTo(nbProjects);
        Assertions.assertThat(jobAllocationStrategyResponse.getJobQueueList().get(0).getMaxSize())
                .isEqualByComparingTo(1);
        Assertions.assertThat(jobAllocationStrategyResponse.getJobQueueList().get(0).getCurrentSize())
                .isEqualByComparingTo(nbThreadForPreviousProject);
        Assertions.assertThat(jobAllocationStrategyResponse.getJobQueueList().get(4).getCurrentSize())
                .isEqualByComparingTo(0);
    }

    @Test
    public void testReturnProjectOk() {
        final IJobAllocationStrategy defaultJobAllocationStrategy = new DefaultJobAllocationStrategy();
        final List<String> projects = new ArrayList<>();
        final List<IJobQueue> jobQueueList = new ArrayList<>();

        jobQueueList.add(new JobQueue("project1", 25, 25));
        jobQueueList.add(new JobQueue("project2", 24, 25));
        jobQueueList.add(new JobQueue("project3", 25, 25));
        jobQueueList.add(new JobQueue("project4", 23, 25));
        jobQueueList.add(new JobQueue("project5", 21, 25));
        projects.add("project1");
        projects.add("project2");
        projects.add("project3");
        projects.add("project4");
        projects.add("project5");

        JobAllocationStrategyResponse jobAllocationStrategyResponse = defaultJobAllocationStrategy
                .getNextQueue(projects, jobQueueList, 125);
        Assertions.assertThat(jobAllocationStrategyResponse.getProjectName()).isEqualTo("project2");
        jobQueueList.set(1, new JobQueue("project2", 25, 25));

        jobAllocationStrategyResponse = defaultJobAllocationStrategy.getNextQueue(projects, jobQueueList, 125);
        Assertions.assertThat(jobAllocationStrategyResponse.getProjectName()).isEqualTo("project4");
        jobQueueList.set(3, new JobQueue("project4", 24, 25));

        jobAllocationStrategyResponse = defaultJobAllocationStrategy.getNextQueue(projects, jobQueueList, 125);
        Assertions.assertThat(jobAllocationStrategyResponse.getProjectName()).isEqualTo("project5");
        jobQueueList.set(4, new JobQueue("project5", 22, 25));

        jobAllocationStrategyResponse = defaultJobAllocationStrategy.getNextQueue(projects, jobQueueList, 125);
        Assertions.assertThat(jobAllocationStrategyResponse.getProjectName()).isEqualTo("project4");
        jobQueueList.set(3, new JobQueue("project4", 25, 25));

        jobAllocationStrategyResponse = defaultJobAllocationStrategy.getNextQueue(projects, jobQueueList, 125);
        Assertions.assertThat(jobAllocationStrategyResponse.getProjectName()).isEqualTo("project5");
        jobQueueList.set(4, new JobQueue("project5", 23, 25));

    }
}
