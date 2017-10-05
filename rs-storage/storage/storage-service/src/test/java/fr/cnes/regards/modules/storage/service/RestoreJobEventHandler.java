package fr.cnes.regards.modules.storage.service;

import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;

public class RestoreJobEventHandler implements IHandler<JobEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(RestoreJobEventHandler.class);

    private final Set<UUID> jobSucceeds = Sets.newHashSet();

    private boolean failed = false;

    @Override
    public void handle(TenantWrapper<JobEvent> wrapper) {
        JobEvent event = wrapper.getContent();
        switch (event.getJobEventType()) {
            case ABORTED:
            case FAILED:
                LOG.info("RestoreJobEvent Failure received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                failed = true;
                break;
            case SUCCEEDED:
                LOG.info("RestoreJobEvent Success received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                jobSucceeds.add(event.getJobId());
                break;
            case RUNNING:
                LOG.info("RestoreJobEvent Running received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                break;
            default:
                LOG.info("RestoreJobEvent Unknown");
                break;
        }
        LOG.info("RestoreJobEvent nb of succeed jobs = {}.", jobSucceeds.size());
    }

    public Set<UUID> getJobSucceeds() {
        LOG.info("Get succeed jobs = {}.", jobSucceeds.size());
        jobSucceeds.forEach(j -> LOG.info("Get succeed jobs : {}", j));
        return jobSucceeds;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean pFailed) {
        failed = pFailed;
    }

    public void reset() {
        failed = false;
        jobSucceeds.clear();
    }

}
