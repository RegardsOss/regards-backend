/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.jobs.domain.IEvent;
import fr.cnes.regards.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.StatusInfo;
import fr.cnes.regards.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.modules.jobs.service.systemservice.IJobInfoSystemService;

@Component
public class JobHandler implements IJobHandler {

    /**
     *
     */
    @Value("${regards.microservice.job.shutdownNowInSeconds}")
    private Integer timeoutShutdownNowInSeconds;

    /**
     *
     */
    @Value("${regards.microservice.job.shutdownInHours}")
    private Integer timeoutShutdownInHours;

    /**
     * Retrieve from the configuration file the number of thread for this microservice
     */
    @Value("${regards.microservice.job.max}")
    private Integer maxJobCapacity;

    private static final Logger LOG = LoggerFactory.getLogger(JobHandler.class);

    private ExecutorService executorService;

    private ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean;

    /**
     * Key: jobInfoId, Tuple.x: Tenant name, Tuple.y: Thread instance
     */
    private final Map<Long, CoupleThreadTenantName> threads;

    private final IJobInfoService jobInfoService;

    private final IJobInfoSystemService jobInfoSystemService;

    private JobMonitor jobMonitor;

    public JobHandler(final IJobInfoService pJobInfoService, final IJobInfoSystemService pJobInfoSystemService) {
        threads = new HashMap<>();
        jobInfoService = pJobInfoService;
        jobInfoSystemService = pJobInfoSystemService;
    }

    @PostConstruct
    public void init() {
        // We also store the JobMonitor inside the ThreadPool so we add 1 slot
        final int maxThreadCapacity = maxJobCapacity + 1;
        LOG.info(String.format(
                               "Launching JobHandler with maxThreadCapacity=[%d]; shutdownInHours=[%d]h; shutdownNowInSeconds=[%d]s",
                               maxJobCapacity, timeoutShutdownInHours, timeoutShutdownNowInSeconds));
        threadPoolExecutorFactoryBean = new ThreadPoolExecutorFactoryBean();
        threadPoolExecutorFactoryBean.setBeanName("JobHandler thread pool");
        threadPoolExecutorFactoryBean.setQueueCapacity(maxThreadCapacity);
        executorService = Executors.newFixedThreadPool(maxThreadCapacity, threadPoolExecutorFactoryBean);
        jobMonitor = new JobMonitor(this);
        final Thread jobMonitorThread = threadPoolExecutorFactoryBean.createThread(jobMonitor);
        jobMonitorThread.start();
    }

    /**
     * Store the JobInfo into the database
     *
     * @return StatusInfo of the new JobInfo
     */
    @Override
    public StatusInfo create(final JobInfo pJobInfo) {
        final JobInfo jobInfo = jobInfoService.createJobInfo(pJobInfo);

        return jobInfo.getStatus();
    }

    /**
     * Delete a job: Ensure that the job will be interrupted if it was running and change its status to Aborted
     */
    @Override
    public StatusInfo abort(final JobInfo pJob) {
        final CoupleThreadTenantName tupleThreadTenant = threads.get(pJob.getId());
        JobInfo jobInfo = jobInfoSystemService.findJobInfo(tupleThreadTenant.getTenantName(), pJob.getId());
        StatusInfo resultingStatus = null;
        if (jobInfo != null) {
            final Thread thread = tupleThreadTenant.getThread();
            if (thread.isAlive()) {
                // Try to interrupt manually
                thread.interrupt();
            }
            try {
                // Waiting the end of the process
                thread.join();
                // Set the job state to Aborted
                jobInfo.getStatus().setJobStatus(JobStatus.ABORTED);
            } catch (final InterruptedException e) {
                jobInfo.getStatus().setJobStatus(JobStatus.FAILED);
                LOG.error(String.format("Failed to interumpt the thread for thread %s", jobInfo.toString()), e);
                Thread.currentThread().interrupt();
            }
            jobInfo = jobInfoService.updateJobInfo(jobInfo);
            resultingStatus = jobInfo.getStatus();
        }

        return resultingStatus;
    }

    /**
     * Retrieve the jobInfo, then execute that job.
     */
    @Override
    public StatusInfo execute(final String tenantName, final Long jobInfoId) {
        final JobInfo jobInfo = jobInfoSystemService.findJobInfo(tenantName, jobInfoId);
        StatusInfo resultingStatus = null;
        if (jobInfo != null) {
            boolean hasFailed = true;
            IJob newJob = null;
            try {
                newJob = (IJob) Class.forName(jobInfo.getClassName()).newInstance();
                // Add the queue event to the thread to let it send IEvent the current JobHandler
                newJob.setQueueEvent(jobMonitor.getQueueEvent());
                newJob.setJobInfoId(jobInfoId);
                newJob.setParameters(jobInfo.getParameters());
                hasFailed = false;
                final Thread thread = threadPoolExecutorFactoryBean.createThread(newJob);
                thread.start();
                threads.put(jobInfoId, new CoupleThreadTenantName(tenantName, thread));
            } catch (final InstantiationException e) {
                LOG.error(String.format("Failed to instantiate the class %s", jobInfo.getClassName()), e);
            } catch (final IllegalAccessException e) {
                LOG.error(String.format("IllegalAccessException %s", jobInfo.getClassName()), e);
            } catch (final ClassNotFoundException e) {
                LOG.error(String.format("Class not found %s", jobInfo.getClassName()), e);
            } finally {
                if (hasFailed) {
                    jobInfo.getStatus().setJobStatus(JobStatus.FAILED);
                } else {
                    jobInfo.getStatus().setJobStatus(JobStatus.RUNNING);
                }
                jobInfoSystemService.updateJobInfo(tenantName, jobInfo);
            }
            resultingStatus = jobInfo.getStatus();
        }
        return resultingStatus;
    }

    @Override
    public StatusInfo shutdownNow() {
        return this.shutdownIn(timeoutShutdownNowInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public StatusInfo shutdown() {
        return this.shutdownIn(timeoutShutdownInHours, TimeUnit.HOURS);
    }

    private StatusInfo shutdownIn(final int timeout, final TimeUnit timeUnit) {
        executorService.shutdown();
        boolean doneCorrectly = false;
        try {
            doneCorrectly = executorService.awaitTermination(timeout, timeUnit);
        } catch (final InterruptedException e) {
            LOG.error("Thread interrupted, closing", e);
            Thread.currentThread().interrupt();
        }
        for (final Entry<Long, CoupleThreadTenantName> threadEntry : threads.entrySet()) {
            final Long jobInfoId = threadEntry.getKey();
            final String tenantName = threadEntry.getValue().getTenantName();
            final JobInfo jobInfo = jobInfoSystemService.findJobInfo(tenantName, jobInfoId);
            if (jobInfo != null) {
                if (doneCorrectly) {
                    jobInfo.getStatus().setJobStatus(JobStatus.ABORTED);
                } else {
                    jobInfo.getStatus().setJobStatus(JobStatus.FAILED);
                }
            } else {
                LOG.error(String.format("Unknow jobInfoId [%ld] while shutting down threads", jobInfoId));
            }
        }
        LOG.info("All threads stopped correctly: ", doneCorrectly);
        return null;
    }

    @Override
    public JobInfo getJob(final Long pJobId) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Receive event from jobs (throw JobMonitor)
     */
    @Override
    public void onEvent(final IEvent pEvent) {
        LOG.info(String.format("Received a new event %s", pEvent.getType().toString()));
        final Long jobInfoId = pEvent.getJobInfoId();
        switch (pEvent.getType()) {
            case JOB_PERCENT_COMPLETED:
                final int jobPercentCompleted = (int) pEvent.getData();
                setJobInfoPercentCompleted(jobInfoId, jobPercentCompleted);
                break;
            case SUCCEEDED:
                setJobStatusToSucceed(jobInfoId);
                break;
            case RUN_ERROR:
                setJobStatusToFailed(jobInfoId);
                break;
            default:
                LOG.error(String.format("Unknow event type %s", pEvent.getType().toString()));
        }
    }

    /**
     * @param pJobInfoId
     */
    private void setJobStatusToFailed(final Long pJobInfoId) {
        final CoupleThreadTenantName tupleTenantThread = threads.get(pJobInfoId);
        final String tenantName = tupleTenantThread.getTenantName();
        final JobInfo jobInfo = jobInfoSystemService.findJobInfo(tenantName, pJobInfoId);
        if (jobInfo != null) {
            jobInfo.getStatus().setJobStatus(JobStatus.FAILED);
            jobInfoSystemService.updateJobInfo(tenantName, jobInfo);
        } else {
            LOG.error(String.format("Job not found %d", pJobInfoId));
        }
        threads.remove(pJobInfoId);

    }

    private void setJobStatusToSucceed(final Long pJobInfoId) {
        final CoupleThreadTenantName tupleTenantThread = threads.get(pJobInfoId);
        final String tenantName = tupleTenantThread.getTenantName();
        final JobInfo jobInfo = jobInfoSystemService.findJobInfo(tenantName, pJobInfoId);
        if (jobInfo != null) {
            jobInfo.getStatus().setJobStatus(JobStatus.SUCCEEDED);
            jobInfoSystemService.updateJobInfo(tenantName, jobInfo);
        } else {
            LOG.error(String.format("Job not found %d", pJobInfoId));
        }
        threads.remove(pJobInfoId);
    }

    private void setJobInfoPercentCompleted(final Long pJobInfoId, final int jobAdvancement) {
        LOG.info(String.format("Received a new avancement %d", jobAdvancement));
        final CoupleThreadTenantName tupleTenantThread = threads.get(pJobInfoId);
        final String tenantName = tupleTenantThread.getTenantName();
        final JobInfo jobInfo = jobInfoSystemService.findJobInfo(tenantName, pJobInfoId);
        if (jobInfo != null) {
            jobInfo.getStatus().setPercentCompleted(jobAdvancement);
            jobInfoSystemService.updateJobInfo(tenantName, jobInfo);
        } else {
            LOG.error(String.format("Job not found %d", pJobInfoId));
        }
    }
}
