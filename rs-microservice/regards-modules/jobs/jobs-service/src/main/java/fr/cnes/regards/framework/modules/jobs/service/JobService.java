package fr.cnes.regards.framework.modules.jobs.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.domain.event.StopJobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobWorkspaceException;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * IJObService implementation
 * @author oroussel
 */
@Service
@RefreshScope
public class JobService implements IJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    /**
     * A BiMap between job id (UUID) and Job (Runnable, in fact RunnableFuture&lt;Void>)
     */
    private final BiMap<JobInfo, RunnableFuture<Void>> jobsMap = Maps.synchronizedBiMap(HashBiMap.create());

    /**
     * A set containing ids of Jobs asked to be stopped whereas they haven't still be launched
     */
    private final Set<UUID> abortedBeforeStartedJobs = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    private IWorkspaceService workspaceService;

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

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    private ThreadPoolExecutor threadPool;

    private static void printStackTrace(JobStatusInfo statusInfo, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        statusInfo.setStackTrace(sw.toString());
    }

    /**
     * Destroy or refresh
     */
    @PreDestroy
    public void preDestroy() {
        LOGGER.info("Shutting down job thread pool...");
        threadPool.shutdown();
        LOGGER.info("Waiting 60s max for jobs to be terminated...");
        try {
            threadPool.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Waiting task interrupted");
        }
    }

    @PostConstruct
    public void init() {
        threadPool = new JobThreadPoolExecutor(poolSize, jobInfoService, jobsMap, runtimeTenantResolver, publisher);
        LOGGER.info("JobService created/refreshed with poolSize: {}", poolSize);
    }

    @Override
    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StopJobEvent.class, new StopJobHandler());
    }

    @Override
    @Async
    public void manage() {
        // To avoid starvation, loop on each tenant before executing jobs
        while (true) {
            boolean noJobAtAll = true;
            for (String tenant : tenantResolver.getAllActiveTenants()) {
                runtimeTenantResolver.forceTenant(tenant);
                // Wait for availability of pool if it is overbooked
                while (threadPool.getActiveCount() >= threadPool.getMaximumPoolSize()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        LOGGER.error("Thread sleep has been interrupted, looks like it's the beginning "
                                   + "of the end, pray for your soul", e);
                    }
                }
                // Find highest priority job to execute
                try {
                    JobInfo jobInfo = jobInfoService.findHighestPriorityQueuedJobAndSetAsToBeRun();
                    if (jobInfo != null) {
                        noJobAtAll = false;
                        jobInfo.setTenant(tenant);
                        this.execute(jobInfo);
                    }
                } catch (RuntimeException e) {
                    LOGGER.warn("Database access problem, skipping and will try later...", e);
                }
            }
            if (noJobAtAll) {
                // No job to execute on any tenants, take a rest
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error("Thread sleep has been interrupted, looks like it's the beginning "
                                         + "of the end, pray for your soul", e);
                }
            }
        }
    }

    /**
     * Periodicaly update all percent completed and estimated completion date of running jobs
     */
    @Scheduled(fixedDelayString = "${regards.jobs.completion.update.rate.ms:1000}")
    @Override
    public void updateCurrentJobsCompletions() {
        // Retrieve all jobInfos of which completion has changed
        Set<JobInfo> toUpdateJobInfos = Sets.filter(jobsMap.keySet(), j -> j.getStatus().hasCompletionChanged());
        if (!toUpdateJobInfos.isEmpty()) {
            // Create a multimap { tenant, (jobInfos) } // NOSONAR
            HashMultimap<String, JobInfo> tenantJobInfoMultimap = HashMultimap.create();
            for (JobInfo jobInfo : toUpdateJobInfos) {
                tenantJobInfoMultimap.put(jobInfo.getTenant(), jobInfo);
            }
            // For each tenant -> (jobInfo) update them
            for (Map.Entry<String, Collection<JobInfo>> entry : tenantJobInfoMultimap.asMap().entrySet()) {
                runtimeTenantResolver.forceTenant(entry.getKey());
                // Direct Update concerned properties into Database whithout changing anything else
                jobInfoService.updateJobInfosCompletion(entry.getValue());
            }
            // Clear completion status
            toUpdateJobInfos.forEach(j -> j.getStatus().clearCompletionChanged());
        }
    }

    @SuppressWarnings("unchecked")
    public void execute(JobInfo jobInfo) {
        try {
            // Case expiration date reached
            if ((jobInfo.getExpirationDate() != null) && jobInfo.getExpirationDate().isBefore(OffsetDateTime.now())) {
                runtimeTenantResolver.forceTenant(jobInfo.getTenant());
                jobInfo.updateStatus(JobStatus.FAILED);
                jobInfo.getStatus().setStackTrace("Expiration date reached");
                jobInfoService.save(jobInfo);
                publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.FAILED));
                return;
            }
            // Case job aborted before its execution
            if (abortedBeforeStartedJobs.contains(jobInfo.getId())) {
                runtimeTenantResolver.forceTenant(jobInfo.getTenant());
                jobInfo.updateStatus(JobStatus.ABORTED);
                jobInfoService.save(jobInfo);
                publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.ABORTED));
                return;
            }
            // First, instantiate job
            @SuppressWarnings("rawtypes") IJob job = (IJob) Class.forName(jobInfo.getClassName()).newInstance();
            beanFactory.autowireBean(job);
            job.setParameters(jobInfo.getParametersAsMap());
            if (job.needWorkspace()) {
                job.setWorkspace(workspaceService::getPrivateDirectory);
            }
            jobInfo.setJob(job);
            // Run job (before executing Job, JobThreadPoolExecutor save JobInfo, have a look if you don't believe me)
            jobsMap.put(jobInfo, (RunnableFuture<Void>) threadPool.submit(job));
        } catch (RejectedExecutionException e) {
            // ThreadPool has been shutted down (maybe due to a refresh)
            LOGGER.warn("Job thread pool rejects job {}", jobInfo.getId());
            resetJob(jobInfo);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Unable to instantiate job", e);
            manageJobInstantiationError(jobInfo, e);
        } catch (JobWorkspaceException e) {
            LOGGER.error("Unable to set workspace", e);
            manageJobInstantiationError(jobInfo, e);
        } catch (JobParameterMissingException e) {
            LOGGER.error("Missing parameter", e);
            manageJobInstantiationError(jobInfo, e);
        } catch (JobParameterInvalidException e) {
            LOGGER.error("Invalid parameter", e);
            manageJobInstantiationError(jobInfo, e);
        }
    }

    /**
     * Job has been rejected by thread pool so reset to its previous state to be taken into account later
     * @param jobInfo
     */
    private void resetJob(JobInfo jobInfo) {
        jobInfo.updateStatus(JobStatus.QUEUED);
        jobInfo.setTenant(null);
        jobInfoService.save(jobInfo);
    }

    /**
     * Manage an error while instantiation Job, like invalid parameters, ClassNotFOundException, etc....
     */
    private void manageJobInstantiationError(JobInfo jobInfo, Exception e) {
        jobInfo.updateStatus(JobStatus.FAILED);
        printStackTrace(jobInfo.getStatus(), e);
        jobInfoService.save(jobInfo);
        publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.FAILED));
    }

    private class StopJobHandler implements IHandler<StopJobEvent> {

        @Override
        public void handle(TenantWrapper<StopJobEvent> wrapper) {
            if (wrapper.getContent() != null) {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                JobService.this.abort(wrapper.getContent().getJobId());
            }
        }
    }

    /**
     * Ask for job abortion if current thread pool is responsible of job execution
     * @param jobId job id
     */
    private void abort(UUID jobId) {
        JobInfo jobInfo = jobInfoService.retrieveJob(jobId);
        if (jobInfo != null) {
            // Check job is currently running
            JobStatus status = jobInfo.getStatus().getStatus();
            switch (status) {
                case RUNNING:
                    // Check if current microservice is running this job
                    if (jobsMap.containsKey(jobInfo)) {
                        RunnableFuture<Void> task = jobsMap.get(jobInfo);
                        task.cancel(true);
                    }
                    break;
                case PENDING: // even a PENDING Job must be set at ABORTED status to avoid a third party service to
                    // set it at QUEUED
                case QUEUED:
                    // Update to ABORTED status (this avoids this job to be taken into account)
                    jobInfo.updateStatus(JobStatus.ABORTED);
                    jobInfoService.save(jobInfo);
                    publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.ABORTED));
                    break;
                case TO_BE_RUN:
                    // Job not yet running
                    abortedBeforeStartedJobs.add(jobInfo.getId());
                    break;
                default:
                    break;
            }
        }
    }

}
