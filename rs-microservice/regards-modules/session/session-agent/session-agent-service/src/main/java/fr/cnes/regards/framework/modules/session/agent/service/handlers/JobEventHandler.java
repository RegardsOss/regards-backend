package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.session.sessioncommons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SnapshotProcess;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author Iliana Ghazali
 **/
public class JobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<JobEvent>  {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private ISnapshotProcessRepository snapshotRepo;

    @Autowired
    private IJobInfoRepository jobInfo;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public boolean validate(String tenant, JobEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<JobEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.info("[JOB EVENT HANDLER] Handling {} JobEvents...", messages.size());
            long start = System.currentTimeMillis();
            handle(messages);
            LOGGER.info("[JOB EVENT HANDLER] {} JobEvents handled in {} ms", messages.size(),
                        System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Handle event by calling the listener method associated to the event type.
     *
     * @param events {@link JobEvent}s
     */
    private void handle(List<JobEvent> events) {
        for(JobEvent jobEvent : events) {
            if(jobEvent.getJobEventType().equals(JobEventType.SUCCEEDED)) {
                UUID jobId = jobEvent.getJobId();
                Optional<SnapshotProcess> snapshotOpt = this.snapshotRepo.findByJobId(jobId);
                Optional<JobInfo> jobInfo = this.jobInfo.findById(jobId);
                if(snapshotOpt.isPresent() && jobInfo.isPresent()) {
                    SnapshotProcess snapshot = snapshotOpt.get();
                    snapshot.setJobId(null);
                    snapshot.setLastUpdate(jobInfo.get().getStatus().getStopDate());
                }
            }
        }
    }
}
