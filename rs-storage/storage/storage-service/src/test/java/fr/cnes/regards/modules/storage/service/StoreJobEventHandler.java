/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;

public class StoreJobEventHandler implements IHandler<JobEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(StoreJobEventHandler.class);

    private Set<UUID> jobSucceeds = Sets.newHashSet();

    private boolean failed = false;

    @Override
    public synchronized void handle(TenantWrapper<JobEvent> wrapper) {
        JobEvent event = wrapper.getContent();
        switch (event.getJobEventType()) {
            case ABORTED:
            case FAILED:
                LOG.info("JobEvent Failure received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                failed = true;
                break;
            case SUCCEEDED:
                LOG.info("JobEvent Success received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                jobSucceeds.add(event.getJobId());
                break;
            case RUNNING:
                LOG.info("JobEvent Running received by test handler. Job uuid={}",
                         wrapper.getContent().getJobId().toString());
                break;
            default:
                break;
        }
        LOG.info("JobEvent nb of succeed jobs = {}. class id={}", jobSucceeds.size(), this.hashCode());
    }

    public synchronized Set<UUID> getJobSucceeds() {
        LOG.info("Get succeed jobs = {}", jobSucceeds.size());
        return jobSucceeds;
    }

    public synchronized void setJobSucceeds(Set<UUID> pJobSucceeds) {
        LOG.info("Set succeed jobs = {}", jobSucceeds.size());
        jobSucceeds = pJobSucceeds;
    }

    public synchronized boolean isFailed() {
        return failed;
    }

    public synchronized void setFailed(boolean pFailed) {
        failed = pFailed;
    }

    public synchronized void reset() {
        failed = false;
        jobSucceeds.clear();
    }

}
