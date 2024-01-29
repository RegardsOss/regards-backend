/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.service;

import com.google.common.collect.BiMap;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.log.CorrelationIdUtils;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Job specific ThreadPoolExecutor.
 * Update JobInfo status between and after execution of associated job
 *
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JobThreadPoolExecutor.class);

    /**
     * Only for the thread names
     */
    private static final ThreadFactory THREAD_FACTORY = new DefaultJobThreadFactory();

    private final IJobInfoService jobInfoService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final BiMap<JobInfo, RunnableFuture<Void>> jobsMap;

    private final IPublisher publisher;

    private final Executor singleThreadExecutor = Executors.newSingleThreadExecutor();

    public JobThreadPoolExecutor(int poolSize,
                                 IJobInfoService jobInfoService,
                                 BiMap<JobInfo, RunnableFuture<Void>> jobsMap,
                                 IRuntimeTenantResolver runtimeTenantResolver,
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
        jobInfo.setLastHeartbeatDate(OffsetDateTime.now());
        jobInfoService.save(jobInfo);
        publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.RUNNING, jobInfo.getClassName()));
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        JobInfo jobInfo = getJobInfo(r);

        if (jobInfo == null) {
            LOGGER.error("Cannot retrieve job info", t);
            return;
        }

        runtimeTenantResolver.forceTenant(jobInfo.getTenant());
        // FutureTask, employed by ThreadPoolExecutor, are unable to manage thrown exceptions.
        // We must handle them explicitly.
        if ((t == null) && (r instanceof Future<?>)) {
            try {
                ((Future<?>) r).get();
            } catch (CancellationException ce) {
                // Following the completion of the job, it is necessary to initiate a new thread
                // for updating the database to prevent potential interruptions at inappropriate times.
                t = ce;
                handleCancellation(jobInfo);
            } catch (ExecutionException ee) {
                t = ee.getCause();
                if (t instanceof CancellationException) {
                    LOGGER.debug("Cancellation triggered by the job");
                    handleCancellation(jobInfo);
                } else {
                    LOGGER.error("Job failed", t);
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    singleThreadExecutor.execute(() -> {
                        runtimeTenantResolver.forceTenant(jobInfo.getTenant());
                        jobInfo.updateStatus(JobStatus.FAILED);
                        jobInfo.getStatus().setStackTrace(sw.toString());
                        jobInfoService.save(jobInfo);

                        publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.FAILED, jobInfo.getClassName()));
                    });
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        // If no error, we don't create a new thread as there should not be any issues
        if (t == null) {
            jobInfo.updateStatus(JobStatus.SUCCEEDED);
            jobInfo.setResult(jobInfo.getJob().getResult());
            jobInfoService.save(jobInfo);

            publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.SUCCEEDED, jobInfo.getClassName()));
        }
        // Delete complete workspace dir if job has one
        if (jobInfo.getJob().needWorkspace()) {
            FileSystemUtils.deleteRecursively(jobInfo.getJob().getWorkspace().toFile());
        }
        CorrelationIdUtils.clearCorrelationId();
        // Clean jobsMap
        jobsMap.remove(jobInfo);
    }

    private void handleCancellation(JobInfo jobInfo) {
        singleThreadExecutor.execute(() -> {
            runtimeTenantResolver.forceTenant(jobInfo.getTenant());
            LOGGER.debug("Job aborted");
            jobInfo.updateStatus(JobStatus.ABORTED);
            jobInfoService.save(jobInfo);
            publisher.publish(new JobEvent(jobInfo.getId(), JobEventType.ABORTED, jobInfo.getClassName()));
        });
    }

}
