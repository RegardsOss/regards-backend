/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.manager;

import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobResult;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;

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
//                    sendEvent(EventType.JOB_PERCENT_COMPLETED, i * 10);
                    Thread.sleep(800);
                } catch (final InterruptedException e) {
                    LOG.warn("Thread interrupted, closing", e);
                    return;
                }
            } else {
                LOG.warn("Thread interrupted, closing");
                return;
            }
        }
        //            sendEvent(EventType.SUCCEEDED);
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Set<JobResult> getResults() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobStatusInfo getStatus() {
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

    @Override
    public void setParameters(Set<JobParameter> pParameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        parameters = pParameters;

    }

}
