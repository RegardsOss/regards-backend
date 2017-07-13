package fr.cnes.regards.framework.modules.jobs.service;

import javax.annotation.PostConstruct;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * IJObService implementation
 * @author oroussel
 */
public class JobService implements IJobService {
    public static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    @Autowired
    private IJobInfoService jobInfoService;

    /**
     * All tenants resolver
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * Current tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Value("${regards.jobs.pool.size:10}")
    private int poolSize;

    private ThreadPoolExecutor threadPool;

    private Map<UUID, Future<?>> jobsMap = new HashMap<>();

    @PostConstruct
    private void init() {
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
    }

    @Override
    @Async
    public void manage() {
        // To avoid starvation, loop on each tenant before executing jobs
        while (true) {
            for (String tenant : tenantResolver.getAllActiveTenants()) {
                runtimeTenantResolver.forceTenant(tenant);
                // Wait for availability of pool if it is overbooked
                while (threadPool.getPoolSize() == threadPool.getMaximumPoolSize()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        LOGGER.error("Thread sleep has been interrupted, looks like it's the beginning of the end, pray "
                                             + "for your soul", e);
                    }
                }
                // Find highest priority job to execute
                JobInfo jobInfo = jobInfoService.findHighestPriorityPendingJob();
                if (jobInfo != null) {
                    jobInfo.updateStatus(JobStatus.QUEUED);
                    jobInfoService.save(jobInfo);
                    this.execute(jobInfo);
                }
            }
        }
    }

    private static void printStackTrace(JobStatusInfo statusInfo, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        statusInfo.setStackTrace(sw.toString());
    }

    public void execute(JobInfo jobInfo) {
        // First, instantiate job
        JobStatus jobStatus;
        try {
            IJob job = (IJob)Class.forName(jobInfo.getClassName()).newInstance();
            job.setId(jobInfo.getId());
            job.setParameters(jobInfo.getParameters());
            job.setWorkspace(jobInfo.getWorkspace());
            // Run job
            jobsMap.put(jobInfo.getId(), threadPool.submit(job));
            jobStatus = JobStatus.RUNNING;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Unable to instantiate job", e);
            jobStatus = JobStatus.FAILED;
            printStackTrace(jobInfo.getStatus(), e);
        } catch (JobParameterMissingException e) {
            LOGGER.error("Missing parameter", e);
            jobStatus = JobStatus.FAILED;
            printStackTrace(jobInfo.getStatus(), e);
        } catch (JobParameterInvalidException e) {
            LOGGER.error("Invalid parameter", e);
            jobStatus = JobStatus.FAILED;
            printStackTrace(jobInfo.getStatus(), e);
        }
        jobInfo.updateStatus(jobStatus);
    }


}
