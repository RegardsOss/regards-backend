package fr.cnes.regards.framework.modules.jobs.service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.*;
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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * IJObService implementation
 * @author oroussel
 */
@Service
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

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    private ThreadPoolExecutor threadPool;

    /**
     * A BiMap between job id (UUID) and Job (Runnable, in fact RunnableFuture&lt;Void>)
     */
    private BiMap<JobInfo, RunnableFuture<Void>> jobsMap = Maps.synchronizedBiMap(HashBiMap.create());

    /**
     * A set containing ids of Jobs asked to be stopped whereas they haven't still be launched
     */
    private Set<UUID> abortedBeforeStartedJobs = Collections.synchronizedSet(new HashSet<>());

    @PostConstruct
    private void init() {
        threadPool = new JobThreadPoolExecutor(poolSize, jobInfoService, jobsMap, runtimeTenantResolver, publisher);
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
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
                        LOGGER.error(
                                "Thread sleep has been interrupted, looks like it's the beginning of the end, pray "
                                        + "for your soul", e);
                    }
                }
                // Find highest priority job to execute
                JobInfo jobInfo = jobInfoService.findHighestPriorityQueuedJobAndSetAsRunning();
                if (jobInfo != null) {
                    noJobAtAll = false;
                    jobInfo.setTenant(tenant);
                    this.execute(jobInfo);
                }
            }
            if (noJobAtAll) {
                // No job to execute on any tenants, take a rest
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ok, i have no problem with that
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
            // Create a multimap { tenant, (jobInfos) }
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

    private static void printStackTrace(JobStatusInfo statusInfo, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        statusInfo.setStackTrace(sw.toString());
    }

    public void execute(JobInfo jobInfo) {
        try {
            // Case job aborted before its execution
            if (abortedBeforeStartedJobs.contains(jobInfo.getId())) {
                runtimeTenantResolver.forceTenant(jobInfo.getTenant());
                jobInfo.updateStatus(JobStatus.ABORTED);
                jobInfoService.save(jobInfo);
                publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.ABORTED));
                return;
            }
            // First, instantiate job
            IJob job = (IJob) Class.forName(jobInfo.getClassName()).newInstance();
            beanFactory.autowireBean(job);
            job.setId(jobInfo.getId());
            job.setParameters(jobInfo.getParameters());
            if (job.needWorkspace()) {
                job.setWorkspace(Files.createTempDirectory(jobInfo.getId().toString()));
            }
            jobInfo.setJob(job);
            // Run job (before executing Job, JobThreadPoolExecutor save JobInfo, have a look if you don't believe me)
            jobsMap.put(jobInfo, (RunnableFuture<Void>) threadPool.submit(job));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
            LOGGER.error("Unable to instantiate job", e);
            jobInfo.updateStatus(JobStatus.FAILED);
            printStackTrace(jobInfo.getStatus(), e);
        } catch (JobParameterMissingException e) {
            LOGGER.error("Missing parameter", e);
            jobInfo.updateStatus(JobStatus.FAILED);
            printStackTrace(jobInfo.getStatus(), e);
        } catch (JobParameterInvalidException e) {
            LOGGER.error("Invalid parameter", e);
            jobInfo.updateStatus(JobStatus.FAILED);
            printStackTrace(jobInfo.getStatus(), e);
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
                case PENDING:
                case QUEUED:
                    // Job not yet running
                    abortedBeforeStartedJobs.add(jobInfo.getId());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Handler for StopEventJob
     */
    private class StopJobHandler implements IHandler<StopJobEvent> {

        @Override
        public void handle(TenantWrapper<StopJobEvent> wrapper) {
            if (wrapper.getContent() != null) {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                JobService.this.abort(wrapper.getContent().getJobId());
            }
        }
    }
}
