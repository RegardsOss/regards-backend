package fr.cnes.regards.framework.modules.jobs.service;

import com.google.common.collect.*;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * IJObService implementation
 *
 * @author oroussel
 */
@Service
public class JobService implements IJobService, InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    public static final long HEARTBEAT_DELAY = 60_000L;

    /**
     * A BiMap between job id (UUID) and Job (Runnable, in fact RunnableFuture&lt;Void>)
     */
    private static final BiMap<JobInfo, RunnableFuture<Void>> jobsMap = Maps.synchronizedBiMap(HashBiMap.create());

    /**
     * A set containing ids of Jobs asked to be stopped whereas they haven't still be launched
     */
    private final Set<UUID> abortedBeforeStartedJobs = Collections.synchronizedSet(new HashSet<>());

    private final IWorkspaceService workspaceService;

    private final IJobInfoService jobInfoService;

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    @Value("${regards.jobs.pool.size:10}")
    private int poolSize;

    @Value("${regards.jobs.scan.delay:1000}")
    private int scanDelay;

    private final ISubscriber subscriber;

    private final IPublisher publisher;

    private final AutowireCapableBeanFactory beanFactory;

    private ThreadPoolExecutor threadPool;

    // Boolean permitting to determine if method manage() can pull jobs and executing them
    private boolean canManage = true;

    public JobService(IWorkspaceService workspaceService,
                      IJobInfoService jobInfoService,
                      ITenantResolver tenantResolver,
                      IRuntimeTenantResolver runtimeTenantResolver,
                      ISubscriber subscriber,
                      IPublisher publisher,
                      AutowireCapableBeanFactory beanFactory) {
        this.workspaceService = workspaceService;
        this.jobInfoService = jobInfoService;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.publisher = publisher;
        this.beanFactory = beanFactory;
    }

    private static void printStackTrace(JobStatusInfo statusInfo, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        statusInfo.setStackTrace(sw.toString());
    }

    /**
     * Destroy or refresh
     */
    @Override
    public void destroy() {
        subscriber.unsubscribeFrom(StopJobEvent.class, false);
        LOGGER.info("Shutting down job thread pool...");
        // Avoid pulling new jobs
        canManage = false;
        stopThreadPool();
    }

    /**
     * Thread pool is built at postConstruct pĥase to have poolSize filled BUT before manage is called by JobInitializer
     */
    @Override
    public void afterPropertiesSet() {
        threadPool = new JobThreadPoolExecutor(poolSize, jobInfoService, jobsMap, runtimeTenantResolver, publisher);
    }

    @Override
    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StopJobEvent.class, new StopJobHandler(this, runtimeTenantResolver));
    }

    /**
     * After a refresh, canManage is automatically reset to true and this method is magically re-executed as soon as
     * necessary even if JobInitializer isn't @RefreshScope'd
     */
    @Override
    @Async
    public Future<Void> manage() {
        // To avoid starvation, loop on each tenant before executing jobs
        while (canManage) {
            boolean noJobAtAll = true;
            try {
                if (!threadPool.isShutdown()) {
                    for (String tenant : tenantResolver.getAllActiveTenants()) {
                        if (!MaintenanceManager.getMaintenance(tenant)) {
                            runtimeTenantResolver.forceTenant(tenant);
                            // Wait for availability of pool if it is overbooked
                            while (threadPool.getActiveCount() >= threadPool.getMaximumPoolSize()) {
                                Thread.sleep(scanDelay);
                            }
                            // Find highest priority job to execute
                            JobInfo jobInfo = jobInfoService.findHighestPriorityQueuedJobAndSetAsToBeRun();
                            if (jobInfo != null) {
                                LOGGER.debug("Job found {}", jobInfo.getId());
                                noJobAtAll = false;
                                jobInfo.setTenant(tenant);
                                this.execute(jobInfo);
                            } else {
                                LOGGER.debug("No job to run yet");
                            }
                        } else {
                            LOGGER.warn("Jobs are currently disabled for tenant {} cause maintenance mode is "
                                        + "activated.", tenant);
                        }
                    }
                }
                if (noJobAtAll) {
                    // No job to execute on any tenants, take a rest
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                LOGGER.error("Thread sleep has been interrupted, looks like it's the beginning "
                             + "of the end, pray for your soul", e);
                break;
            } catch (Exception e) {
                LOGGER.warn("Unexpected error occurred on service poller, ignoring error.", e);
                try {
                    // Wait a little bit before trying again
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    LOGGER.error("Thread sleep has been interrupted, looks like it's the beginning "
                                 + "of the end, pray for your soul", ie);
                    break;
                }
            }
        }
        LOGGER.warn("Job service puller just died");
        return new AsyncResult<>(null);
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
            // Create a multimap { tenant, (jobInfos) }
            HashMultimap<String, JobInfo> tenantJobInfoMultimap = HashMultimap.create();
            for (JobInfo jobInfo : toUpdateJobInfos) {
                tenantJobInfoMultimap.put(jobInfo.getTenant(), jobInfo);
            }
            // For each tenant -> (jobInfo) update them
            for (Map.Entry<String, Collection<JobInfo>> entry : tenantJobInfoMultimap.asMap().entrySet()) {
                runtimeTenantResolver.forceTenant(entry.getKey());
                // Direct Update concerned properties into Database without changing anything else
                jobInfoService.updateJobInfosCompletion(entry.getValue());
            }
            // Clear completion status
            toUpdateJobInfos.forEach(j -> j.getStatus().clearCompletionChanged());
        }
    }

    @Scheduled(fixedDelay = HEARTBEAT_DELAY)
    @Override
    public void jobsHeartbeat() {
        // Retrieve all jobInfos of which completion has changed
        Set<JobInfo> stillAliveJobInfos = jobsMap.keySet();
        if (!stillAliveJobInfos.isEmpty()) {
            // Create a multimap { tenant, (jobInfosIds) } // NOSONAR
            HashMultimap<String, UUID> tenantJobInfoMultimap = HashMultimap.create();
            for (JobInfo jobInfo : stillAliveJobInfos) {
                tenantJobInfoMultimap.put(jobInfo.getTenant(), jobInfo.getId());
            }
            // For each tenant -> (jobInfoIds) update them
            for (Map.Entry<String, Collection<UUID>> entry : tenantJobInfoMultimap.asMap().entrySet()) {
                runtimeTenantResolver.forceTenant(entry.getKey());
                // Direct Update concerned properties into Database whithout changing anything else
                jobInfoService.updateJobInfosHeartbeat(entry.getValue());
            }
        }
        // Always update last pingUpdateDate
        jobInfoService.updateLastJobsPingDate();
    }

    @Override
    public RunnableFuture<Void> runJob(JobInfo jobInfo, String tenant) {
        jobInfo.setTenant(tenant);
        return this.execute(jobInfo);
    }

    @SuppressWarnings("unchecked")
    public RunnableFuture<Void> execute(JobInfo jobInfo) {
        RunnableFuture<Void> future = null;
        if (jobsMap.containsKey(jobInfo)) {
            LOGGER.warn("Job {} already running", jobInfo.getId());
            return jobsMap.get(jobInfo);
        }
        try {
            // we force tenant in all cases even if everything is good there is no need to.
            // forced tenant is necessary when updating database so for the following cases:
            // expired job, aborted job, instantiation errors and job resetting
            runtimeTenantResolver.forceTenant(jobInfo.getTenant());
            // Case expiration date reached
            if ((jobInfo.getExpirationDate() != null) && jobInfo.getExpirationDate().isBefore(OffsetDateTime.now())) {
                jobInfo.updateStatus(JobStatus.FAILED);
                jobInfo.getStatus().setStackTrace("Expiration date reached");
                jobInfoService.save(jobInfo);
                publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.FAILED));
                return null;
            }
            // Case job aborted before its execution
            if (abortedBeforeStartedJobs.contains(jobInfo.getId())) {
                runtimeTenantResolver.forceTenant(jobInfo.getTenant());
                jobInfo.updateStatus(JobStatus.ABORTED);
                jobInfoService.save(jobInfo);
                publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.ABORTED));
                return null;
            }
            // First, instantiate job
            @SuppressWarnings("rawtypes") IJob job = (IJob) Class.forName(jobInfo.getClassName())
                                                                 .getConstructor()
                                                                 .newInstance();
            beanFactory.autowireBean(job);
            job.setJobInfoId(jobInfo.getId());
            job.setParameters(jobInfo.getParametersAsMap());
            if (job.needWorkspace()) {
                job.setWorkspace(workspaceService::getPrivateDirectory);
            }
            jobInfo.setJob(job);
            // Run job (before executing Job, JobThreadPoolExecutor save JobInfo, have a look if you don't believe me)
            future = (RunnableFuture<Void>) threadPool.submit(job);
            // Initiate first heart beat of job
            jobInfo.setLastHeartbeatDate(OffsetDateTime.now());
            jobsMap.put(jobInfo, future);
            return future;
        } catch (RejectedExecutionException e) {
            // ThreadPool has been shutted down (maybe due to a refresh)
            LOGGER.warn("Job thread pool rejects job {}", jobInfo.getId());
            resetJob(jobInfo);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | InvocationTargetException |
                 IllegalAccessException e) {
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
        } finally {
            runtimeTenantResolver.clearTenant();
        }
        return future;
    }

    public void cleanAndRestart() {
        stopThreadPool();
        this.afterPropertiesSet();
        LOGGER.info("JOB Service reinitialized and all jobs stopped !");
    }

    private void stopThreadPool() {
        List<Runnable> runnableTasks = threadPool.shutdownNow();
        if (!runnableTasks.isEmpty()) {
            LOGGER.info("Waiting 60s max for {} jobs to be terminated...", runnableTasks.size());
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOGGER.error("Terminating job thread pool executor. Jobs were not finished with 1min timeout");
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Waiting task interrupted");
            }
        }
        threadPool = null;
    }

    /**
     * Job has been rejected by thread pool so reset to its previous state to be taken into account later
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

    /**
     * Ask for job abortion if current thread pool is responsible of job execution
     *
     * @param jobId job id
     */
    private void abort(UUID jobId) {
        JobInfo jobInfo = jobInfoService.retrieveJob(jobId);
        if (jobInfo != null) {
            // Check job is currently running
            JobStatus status = jobInfo.getStatus().getStatus();
            switch (status) {
                case RUNNING -> {
                    // Check if current microservice is running this job
                    if (jobsMap.containsKey(jobInfo)) {
                        LOGGER.info("Aborting running job {}", jobId);
                        RunnableFuture<Void> task = jobsMap.get(jobInfo);
                        task.cancel(true);
                    } else {
                        LOGGER.debug(
                            "Event received to abort the running job {}, but this job is not running on this instance",
                            jobId);
                    }
                }
                case PENDING, QUEUED -> {
                    // Update to ABORTED status (this avoids this job to be taken into account)
                    // even a PENDING Job must be set at ABORTED status to avoid a third party service to
                    // set it at QUEUED
                    jobInfo.updateStatus(JobStatus.ABORTED);
                    jobInfoService.save(jobInfo);
                    publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.ABORTED));
                }
                case TO_BE_RUN ->
                    // Job not yet running
                    abortedBeforeStartedJobs.add(jobInfo.getId());
                default -> {
                    // Nothing to do
                }
            }
        }
    }

    private record StopJobHandler(JobService jobService,
                                  IRuntimeTenantResolver runtimeTenantResolver) implements IHandler<StopJobEvent> {

        @Override
        public void handle(String tenant, StopJobEvent event) {
            runtimeTenantResolver.forceTenant(tenant);
            jobService.abort(event.getJobId());
            runtimeTenantResolver.clearTenant();
        }
    }

}
