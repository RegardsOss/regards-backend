/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.StatusInfo;

@Component
public class JobHandler implements IJobHandler {

    // Todo retrieve number of async slots in config file
    private static final int maxCapacity = 5;

    private static final Logger LOG = LoggerFactory.getLogger(JobHandler.class);

    private final ExecutorService executorService;

    private final ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean;

    private final Map<Long, Thread> threads;

    private final IJobInfoRepository jobInfoRepository;

    public JobHandler(IJobInfoRepository pJobInfoRepository) {
        threadPoolExecutorFactoryBean = new ThreadPoolExecutorFactoryBean();
        threadPoolExecutorFactoryBean.setBeanName("Job thread pool");
        threadPoolExecutorFactoryBean.setQueueCapacity(maxCapacity);
        executorService = Executors.newFixedThreadPool(maxCapacity, threadPoolExecutorFactoryBean);

        threads = new HashMap<>();
        jobInfoRepository = pJobInfoRepository;
    }

    @Override
    public StatusInfo create(JobInfo pJob) {
        return null;
    }

    /**
     * Delete a job: Ensure that the job will be interrupted if it was running and change its status to Aborted
     */
    @Override
    public StatusInfo delete(JobInfo pJob) {
        Thread thread = threads.get(pJob.getId());
        if (thread.isAlive()) {
            thread.interrupt();
        }
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see create
     */
    @Override
    public StatusInfo execute(Long jobId) {
        IJob newJob = null;
        boolean hasFailed = true;
        JobInfo pJob = null;
        try {
            newJob = (IJob) Class.forName(pJob.getClassName()).newInstance();
            hasFailed = false;
        } catch (InstantiationException e) {
            LOG.info(String.format("Failed to instantiate the class %s", pJob.getClassName()));
        } catch (IllegalAccessException e) {
            LOG.info(String.format("IllegalAccessException %s", pJob.getClassName()));
        } catch (ClassNotFoundException e) {
            LOG.info(String.format("Class not found %s", pJob.getClassName()), e);
        } finally {
            if (hasFailed) {
                pJob.getStatus().setStatus(JobStatus.FAILED);
            } else {
                pJob.getStatus().setStatus(JobStatus.PENDING);
            }
            jobInfoRepository.save(pJob);
        }
        Thread createThread = threadPoolExecutorFactoryBean.createThread(newJob);
        return pJob.getStatus();
    }

    @Override
    public StatusInfo handle(JobInfo pJob) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StatusInfo restart(JobInfo pJob) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StatusInfo stop(Long pJobId) {
        executorService.shutdown();
        boolean doneCorrectly = false;
        try {
            doneCorrectly = executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.warn("Thread interrupted, closing", e);
        }
        LOG.info("All threads stopped correctly: ", doneCorrectly);
        return null;
    }

    @Override
    public JobInfo getJob(Long pJobId) {
        // TODO Auto-generated method stub
        return null;
    }

}
