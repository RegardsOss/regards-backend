package fr.cnes.regards.framework.modules.jobs.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.util.FileSystemUtils;

import com.google.common.collect.BiMap;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.AbortedJobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.FailedJobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.RunningJobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.SucceededJobEvent;
import fr.cnes.regards.framework.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Job specific ThreadPoolExecutor.
 * Update JobInfo status between and after execution of associated job
 * @author oroussel
 */
public class JobThreadPoolExecutor extends ThreadPoolExecutor {

    private IJobInfoService jobInfoService;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private BiMap<JobInfo, RunnableFuture<Void>> jobsMap;

    private IPublisher publisher;

    public JobThreadPoolExecutor(int poolSize, IJobInfoService jobInfoService,
            BiMap<JobInfo, RunnableFuture<Void>> jobsMap, IRuntimeTenantResolver runtimeTenantResolver,
            IPublisher publisher) {
        super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.jobInfoService = jobInfoService;
        this.jobsMap = jobsMap;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.publisher = publisher;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        JobInfo jobInfo = jobsMap.inverse().get(r);
        while (jobInfo == null) {
            jobInfo = jobsMap.inverse().get(r);
            System.out.println("TROP RAPIDE !");
        }
        runtimeTenantResolver.forceTenant(jobInfo.getTenant());
        jobInfo.updateStatus(JobStatus.RUNNING);
        jobInfoService.save(jobInfo);
        publisher.publish(new RunningJobEvent(jobInfo.getId()));
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        JobInfo jobInfo = jobsMap.inverse().get(r);
        runtimeTenantResolver.forceTenant(jobInfo.getTenant());
        // FutureTask (which is used by ThreadPoolExecutor) doesn't give a fuck of thrown exception so we must get it
        // by hands
        if ((t == null) && (r instanceof Future<?>)) {
            try {
                ((Future<?>) r).get();
            } catch (CancellationException ce) {
                t = ce;
                jobInfo.updateStatus(JobStatus.ABORTED);
                jobInfoService.save(jobInfo);
                publisher.publish(new AbortedJobEvent(jobInfo.getId()));
            } catch (ExecutionException ee) {
                t = ee.getCause();
                jobInfo.updateStatus(JobStatus.FAILED);
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                jobInfo.getStatus().setStackTrace(sw.toString());
                jobInfoService.save(jobInfo);
                publisher.publish(new FailedJobEvent(jobInfo.getId()));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        // If no error
        if (t == null) {
            jobInfo.updateStatus(JobStatus.SUCCEEDED);
            jobInfo.getStatus().setPercentCompleted(100);
            jobInfoService.save(jobInfo);
            publisher.publish(new SucceededJobEvent(jobInfo.getId()));
        }
        // Delete complete workspace dir if job has one
        if (jobInfo.getJob().needWorkspace()) {
            FileSystemUtils.deleteRecursively(jobInfo.getJob().getWorkspace().toFile());
        }
    }
}
