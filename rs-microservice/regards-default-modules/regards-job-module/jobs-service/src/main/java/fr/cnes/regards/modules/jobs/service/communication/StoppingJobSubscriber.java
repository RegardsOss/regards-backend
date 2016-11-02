/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.StatusInfo;
import fr.cnes.regards.modules.jobs.service.manager.IJobHandler;

/**
 * @author lmieulet
 */
public class StoppingJobSubscriber implements IHandler<StoppingJobEvent> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(StoppingJobSubscriberTest.class);

    /**
     * the job handler
     */
    private final IJobHandler jobHandler;

    /**
     * @param pJobHandler
     *            the job handler
     */
    public StoppingJobSubscriber(final IJobHandler pJobHandler) {
        jobHandler = pJobHandler;
    }

    @Override
    public void handle(final TenantWrapper<StoppingJobEvent> pStoppingJobEventWrapped) {
        final Long jobInfoId = pStoppingJobEventWrapped.getContent().getJobInfoId();
        final StatusInfo jobInfoAborted = jobHandler.abort(jobInfoId);
        if (jobInfoAborted.getJobStatus().equals(JobStatus.ABORTED)) {
            LOG.info(String.format("Job [%d] correctly stopped", jobInfoId));
        } else {
            LOG.warn(String.format("Job [%d] state [%s] was not stopped correctly", jobInfoId,
                                   jobInfoAborted.getJobStatus().toString()));
        }
    }

}
