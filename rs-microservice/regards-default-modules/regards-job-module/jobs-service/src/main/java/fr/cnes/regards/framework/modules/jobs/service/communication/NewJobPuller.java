/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * @author Léo Mieulet
 */
public class NewJobPuller implements INewJobPuller {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NewJobPuller.class);

    /**
     * Poller instance
     */
    private final Poller poller;

    /**
     * @param pPoller
     *            poller instance
     */
    public NewJobPuller(final Poller pPoller) {
        super();
        poller = pPoller;
    }

    /**
     * @param pProjectName
     *            the project name
     * @return a new jobInfo id
     */
    @Override
    public Long getJob(final String pProjectName) {
        Long jobInfoId = null;
        try {
            final TenantWrapper<NewJobEvent> tenantWrapper = poller.poll(pProjectName, NewJobEvent.class);
            final NewJobEvent newJobEvent = tenantWrapper.getContent();
            jobInfoId = newJobEvent.getJobInfoId();
        } catch (final RabbitMQVhostException e) {
            LOG.error(String.format("Failed to fetch a jobInfo for tenant [%s]", pProjectName), e);
        }
        return jobInfoId;
    }

}
