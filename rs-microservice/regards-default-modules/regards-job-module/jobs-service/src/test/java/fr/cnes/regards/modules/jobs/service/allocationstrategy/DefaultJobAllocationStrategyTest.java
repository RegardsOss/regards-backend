/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.allocationstrategy;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobQueue;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.JobAllocationStrategyResponse;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.JobQueue;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 */
public class DefaultJobAllocationStrategyTest {

    @Test
    public void testInitialisation() {
        IJobAllocationStrategy defaultJobAllocationStrategy = new DefaultJobAllocationStrategy();
        List<Project> pProjects = new ArrayList<>();
        int nbProjects = 9;
        for (int i = 0; i < nbProjects; i++) {
            pProjects.add(new Project("description", "icon", true, String.format("project%d", i)));
        }
        JobAllocationStrategyResponse jobAllocationStrategyResponse = defaultJobAllocationStrategy
                .getNextQueue(pProjects, null, 100);
        Assertions.assertThat(jobAllocationStrategyResponse.getJobQueueList().size()).isEqualByComparingTo(nbProjects);
        Assertions.assertThat(jobAllocationStrategyResponse.getJobQueueList().get(0).getMaxSize())
                .isEqualByComparingTo(12);
    }

    @Test
    public void testScaleWhenNewProjects() {
        IJobAllocationStrategy defaultJobAllocationStrategy = new DefaultJobAllocationStrategy();
        List<Project> projects = new ArrayList<>();
        List<IJobQueue> jobQueueList = new ArrayList<>();

        int nbPreviousQueues = 4;
        int nbThreadForPreviousProject = 7;
        for (int i = 0; i < nbPreviousQueues; i++) {
            jobQueueList.add(new JobQueue(String.format("project%d", i), nbThreadForPreviousProject, 25));
        }

        int nbProjects = 1000;
        for (int i = 0; i < nbProjects; i++) {
            projects.add(new Project("description", "icon", true, String.format("project%d", i)));
        }
        Assertions.assertThat(jobQueueList.size()).isNotEqualTo(projects.size());
        JobAllocationStrategyResponse jobAllocationStrategyResponse = defaultJobAllocationStrategy
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
        IJobAllocationStrategy defaultJobAllocationStrategy = new DefaultJobAllocationStrategy();
        List<Project> projects = new ArrayList<>();
        List<IJobQueue> jobQueueList = new ArrayList<>();

        jobQueueList.add(new JobQueue("project1", 25, 25));
        jobQueueList.add(new JobQueue("project2", 24, 25));
        jobQueueList.add(new JobQueue("project3", 25, 25));
        jobQueueList.add(new JobQueue("project4", 23, 25));
        jobQueueList.add(new JobQueue("project5", 21, 25));
        projects.add(new Project("description", "icon", true, "project1"));
        projects.add(new Project("description", "icon", true, "project2"));
        projects.add(new Project("description", "icon", true, "project3"));
        projects.add(new Project("description", "icon", true, "project4"));
        projects.add(new Project("description", "icon", true, "project5"));

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
