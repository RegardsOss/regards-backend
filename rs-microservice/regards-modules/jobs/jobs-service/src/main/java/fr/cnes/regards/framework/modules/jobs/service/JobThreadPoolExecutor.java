package fr.cnes.regards.framework.modules.jobs.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import com.google.common.collect.BiMap;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Job specific ThreadPoolExecutor.
 * Update JobInfo status between and after execution of associated job
 * @author oroussel
 */
public class JobThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     * The default thread factory
     */
    private static final class DefaultJobThreadFactory implements ThreadFactory {

        private static final AtomicInteger poolNumber = new AtomicInteger(1);

        private final ThreadGroup group;

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final String namePrefix;

        private DefaultJobThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "job-pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);

    /**
     * Only for the thread names
     */
    private static final ThreadFactory THREAD_FACTORY = new DefaultJobThreadFactory();

    private final IJobInfoService jobInfoService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final BiMap<JobInfo, RunnableFuture<Void>> jobsMap;

    private final IPublisher publisher;

    private final Executor singleThreadExecutor = Executors.newSingleThreadExecutor();

    public JobThreadPoolExecutor(int poolSize, IJobInfoService jobInfoService,
            BiMap<JobInfo, RunnableFuture<Void>> jobsMap, IRuntimeTenantResolver runtimeTenantResolver,
            IPublisher publisher) {
        super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), THREAD_FACTORY);
        this.jobInfoService = jobInfoService;
        this.jobsMap = jobsMap;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.publisher = publisher;
    }

    private JobInfo getJobInfo(Runnable r) {
        JobInfo jobInfo = jobsMap.inverse().get(r);
        int loop = 0;
        while ((jobInfo == null) && (loop < 10)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
            loop++;
            jobInfo = jobsMap.inverse().get(r);
        }
        if (jobInfo == null) {
            LOGGER.error("Error getting job from existing runnable tasks.");
        }
        return jobInfo;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        JobInfo jobInfo = getJobInfo(r);
        // In case jobsMap is not yet available (this means afterExecute has been called very very early)
        // because of jobsMap.put(jobInfo, threadPool.submit(...))
        while (jobInfo == null) {
            jobInfo = jobsMap.inverse().get(r);
        }
        runtimeTenantResolver.forceTenant(jobInfo.getTenant());
        jobInfo.updateStatus(JobStatus.RUNNING);
        jobInfoService.save(jobInfo);
        publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.RUNNING));
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        JobInfo jobInfo = getJobInfo(r);
        runtimeTenantResolver.forceTenant(jobInfo.getTenant());
        // FutureTask (which is used by ThreadPoolExecutor) doesn't give a fuck of thrown exception so we must get it
        // by hands
        if ((t == null) && (r instanceof Future<?>)) {
            try {
                ((Future<?>) r).get();
            } catch (CancellationException ce) {
                // after execute being execute in the thread that is ending,
                // to avoid issues with interruption at the wrong moment,
                // we need to create a new thread to update the db
                t = ce;
                singleThreadExecutor.execute(() -> {
                    runtimeTenantResolver.forceTenant(jobInfo.getTenant());
                    jobInfo.updateStatus(JobStatus.ABORTED);
                    jobInfoService.save(jobInfo);
                    publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.ABORTED));
                });
            } catch (ExecutionException ee) {
                t = ee.getCause();
                LOGGER.error("Job failed", t);
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                singleThreadExecutor.execute(() -> {
                    runtimeTenantResolver.forceTenant(jobInfo.getTenant());
                    jobInfo.updateStatus(JobStatus.FAILED);
                    jobInfo.getStatus().setStackTrace(sw.toString());
                    jobInfoService.save(jobInfo);
                    publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.FAILED));
                });
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        // If no error, we don't create a new thread as there should not be any issues
        if (t == null) {
            jobInfo.updateStatus(JobStatus.SUCCEEDED);
            jobInfo.setResult(jobInfo.getJob().getResult());
            jobInfoService.save(jobInfo);
            publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.SUCCEEDED));
        }
        // Delete complete workspace dir if job has one
        if (jobInfo.getJob().needWorkspace()) {
            FileSystemUtils.deleteRecursively(jobInfo.getJob().getWorkspace().toFile());
        }
        // Clean jobsMap
        jobsMap.remove(jobInfo);
    }

}
