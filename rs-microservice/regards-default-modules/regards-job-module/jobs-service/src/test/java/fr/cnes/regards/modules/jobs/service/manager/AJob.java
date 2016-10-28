/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.modules.jobs.domain.EventType;
import fr.cnes.regards.modules.jobs.domain.Output;
import fr.cnes.regards.modules.jobs.domain.StatusInfo;

/**
 *
 */
public class AJob extends AbstractJob {

    private static final Logger LOG = LoggerFactory.getLogger(AJob.class);

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            if (!Thread.currentThread().isInterrupted()) {
                LOG.info("AJob: Waiting..");
                try {
                    sendEvent(EventType.JOB_PERCENT_COMPLETED, i * 10);
                    Thread.sleep(200);
                } catch (final InterruptedException e) {
                    LOG.warn("Thread interrupted, closing", e);
                    return;
                }
            } else {
                LOG.warn("Thread interrupted, closing");
                return;
            }
        }
        try {
            sendEvent(EventType.SUCCEEDED);
        } catch (final InterruptedException e) {
            LOG.error("Failed to send success to parent thread", e);
        }
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<Output> getResults() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StatusInfo getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasResult() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean needWorkspace() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setWorkspace(final Path pPath) {
        // TODO Auto-generated method stub

    }

}
