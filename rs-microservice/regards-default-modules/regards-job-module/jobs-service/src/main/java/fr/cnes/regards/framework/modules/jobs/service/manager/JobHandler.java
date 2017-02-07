/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.manager;

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

import fr.cnes.regards.framework.modules.jobs.domain.IEvent;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.StatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.systemservice.IJobInfoSystemService;
import fr.cnes.regards.framework.modules.jobs.service.systemservice.JobInfoSystemService;

/**
 * Service to manipulate Job and JobInfo Contains a threadPool to manage the lifecycle of our jobs
 *
 * @author Léo Mieulet
 */
@Component
public class JobHandler implements IJobHandler {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JobHandler.class);

    /**
     * Service used when user token unavailable
     */
    private final IJobInfoSystemService jobInfoSystemService;

    /**
     * Store the small delay accepted to let jobs stop by themselves while shutting down instantly
     */
    @Value("${regards.microservice.job.shutdownNowInSeconds}")
    private Integer timeoutShutdownNowInSeconds;

    /**
     * Store the delay while jobs can stop by themselves while shutting down the threadpool
     */
    @Value("${regards.microservice.job.shutdownInHours}")
    private Integer timeoutShutdownInHours;

    /**
     * Retrieve from the configuration file the number of thread for this microservice
     */
    @Value("${regards.microservice.job.max}")
    private Integer maxJobCapacity;

    /**
     * Excecutor service and helpers
     */
    private ExecutorService executorService;

    /**
     * Spring thread pool executor
     */
    private ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean;

    /**
     * Key: jobInfoId, CoupleThreadTenantName(Tenant name, Thread instance)
     */
    private final Map<Long, CoupleThreadTenantName> threads;

    /**
     * Monitor jobs, allows them to send event to JobHandler
     */
    private final JobMonitor jobMonitor;

    /**
     * Constructor with a {@link JobInfoSystemService}
     *
     * @param pJobInfoSystemService
     *            a {@link JobInfoSystemService}
     */
    public JobHandler(final IJobInfoSystemService pJobInfoSystemService) {
        threads = new HashMap<>();
        jobInfoSystemService = pJobInfoSystemService;
        jobMonitor = new JobMonitor(this);
    }

    /**
     * Spring boot shall set the value of JobHandler attributes after the constructor so we create here the thread pool
     */
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
        final Thread jobMonitorThread = threadPoolExecutorFactoryBean.createThread(jobMonitor);
        jobMonitorThread.start();
    }

    @Override
    public StatusInfo abort(final Long pJobInfoId) {
        StatusInfo resultingStatus = null;
        final CoupleThreadTenantName tupleThreadTenant = threads.get(pJobInfoId);
        if (tupleThreadTenant != null) {
            final String tenantName = tupleThreadTenant.getTenantName();
            JobInfo jobInfo = jobInfoSystemService.findJobInfo(tenantName, pJobInfoId);
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
                    LOG.error(String.format("Failed to interumpt the thread for jobInfo %s", jobInfo.toString()), e);
                    Thread.currentThread().interrupt();
                }
                jobInfo = jobInfoSystemService.updateJobInfo(tenantName, jobInfo);
                resultingStatus = jobInfo.getStatus();
            }
        }

        return resultingStatus;
    }

    @Override
    public StatusInfo execute(final String pTenant, final Long pJobInfoId) {
        final JobInfo jobInfo = jobInfoSystemService.findJobInfo(pTenant, pJobInfoId);
        StatusInfo resultingStatus = null;
        if (jobInfo != null) {
            boolean hasFailed = true;
            IJob newJob = null;
            try {
                newJob = (IJob) Class.forName(jobInfo.getClassName()).newInstance();
                // Add the queue event to the thread to let it send IEvent the current JobHandler
                newJob.setQueueEvent(jobMonitor.getQueueEvent());
                newJob.setJobInfoId(pJobInfoId);
                newJob.setTenantName(pTenant);
                newJob.setParameters(jobInfo.getParameters());
                newJob.setWorkspace(jobInfo.getWorkspace());
                hasFailed = false;
                final Thread thread = threadPoolExecutorFactoryBean.createThread(newJob);
                thread.start();
                threads.put(pJobInfoId, new CoupleThreadTenantName(pTenant, thread));
            } catch (final InstantiationException e) {
                LOG.error(String.format("Failed to instantiate the class %s", jobInfo.getClassName()), e);
            } catch (final IllegalAccessException e) {
                LOG.error(String.format("IllegalAccessException %s", jobInfo.getClassName()), e);
            } catch (final ClassNotFoundException e) {
                LOG.error(String.format("Class not found %s", jobInfo.getClassName()), e);
            } catch (JobParameterMissingException | JobParameterInvalidException e) {
                LOG.error(String.format("Could not initialized job parameters id job=<%d>: %s", jobInfo.getId(),
                                        jobInfo.getParameters().toString()),
                          e);
            } finally {
                if (hasFailed) {
                    jobInfo.getStatus().setJobStatus(JobStatus.FAILED);
                } else {
                    jobInfo.getStatus().setJobStatus(JobStatus.RUNNING);
                }
                jobInfoSystemService.updateJobInfo(pTenant, jobInfo);
            }
            resultingStatus = jobInfo.getStatus();
        }
        return resultingStatus;
    }

    @Override
    public void shutdownNow() {
        shutdownIn(timeoutShutdownNowInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {
        shutdownIn(timeoutShutdownInHours, TimeUnit.HOURS);
    }

    @Override
    public void onEvent(final IEvent pEvent) {
        LOG.info(String.format("Received a new event %s", pEvent.getType().toString()));
        final Long jobInfoId = pEvent.getJobInfoId();
        final String tenantName = pEvent.getTenantName();
        switch (pEvent.getType()) {
            case JOB_PERCENT_COMPLETED:
                final int jobPercentCompleted = (int) pEvent.getData();
                setJobInfoPercentCompleted(jobInfoId, tenantName, jobPercentCompleted);
                break;
            case SUCCEEDED:
                setJobStatusTo(jobInfoId, tenantName, JobStatus.SUCCEEDED);
                break;
            case RUN_ERROR:
                setJobStatusTo(jobInfoId, tenantName, JobStatus.FAILED);
                break;
            default:
                LOG.error(String.format("Unknow event type %s", pEvent.getType().toString()));
        }
    }

    /**
     * @return the maxJobCapacity
     */
    @Override
    public Integer getMaxJobCapacity() {
        return maxJobCapacity;
    }

    @Override
    public Map<String, Integer> getNbActiveThreadByTenant() {
        final Map<String, Integer> result = new HashMap<>();
        synchronized (threads) {
            for (final CoupleThreadTenantName threadTenant : threads.values()) {
                if (!result.containsKey(threadTenant.getTenantName())) {
                    result.put(threadTenant.getTenantName(), 0);
                }
                result.put(threadTenant.getTenantName(), result.get(threadTenant.getTenantName()) + 1);
            }
        }
        return result;
    }

    @Override
    public boolean isThreadPoolFull() {
        final Map<String, Integer> nbActiveThreadByTenant = getNbActiveThreadByTenant();
        int nbActiveThread = 0;
        for (final Integer nbThread : nbActiveThreadByTenant.values()) {
            nbActiveThread += nbThread;
        }
        return nbActiveThread >= getMaxJobCapacity();
    }

    /**
     *
     * @param pTimeout
     *            let threads survive during pTimeout
     * @param pTimeUnit
     *            the pTimeout unit
     */
    private void shutdownIn(final int pTimeout, final TimeUnit pTimeUnit) {
        executorService.shutdown();
        boolean doneCorrectly = false;
        try {
            doneCorrectly = executorService.awaitTermination(pTimeout, pTimeUnit);
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
                LOG.error(String.format("Unknow jobInfoId [%d] while shutting down threads", jobInfoId));
            }
        }
        LOG.info("All threads stopped correctly: ", doneCorrectly);
    }

    /**
     *
     * @param pJobInfoId
     *            the jobInfo id
     * @param pTenantName
     * @param pJobStatus
     *            the new jobStatus
     */
    private void setJobStatusTo(final Long pJobInfoId, final String pTenantName, final JobStatus pJobStatus) {
        jobInfoSystemService.updateJobInfoToDone(pJobInfoId, pJobStatus, pTenantName);
        threads.remove(pJobInfoId);
    }

    /**
     *
     * @param pJobInfoId
     *            the jobInfo id
     * @param pTenantName
     * @param pJobAdvancement
     *            the jobInfo advancement
     */
    private void setJobInfoPercentCompleted(final Long pJobInfoId, final String pTenantName,
            final int pJobAdvancement) {
        LOG.info(String.format("Received a new avancement %d", pJobAdvancement));
        final JobInfo jobInfo = jobInfoSystemService.findJobInfo(pTenantName, pJobInfoId);
        if (jobInfo != null) {
            jobInfo.getStatus().setPercentCompleted(pJobAdvancement);
            jobInfoSystemService.updateJobInfo(pTenantName, jobInfo);
        } else {
            LOG.error(String.format("Job not found %d", pJobInfoId));
        }
    }

}
