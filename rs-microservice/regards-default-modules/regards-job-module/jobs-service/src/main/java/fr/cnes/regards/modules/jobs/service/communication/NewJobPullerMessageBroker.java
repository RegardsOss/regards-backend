/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 */
public class NewJobPullerMessageBroker implements INewJobPullerMessageBroker {

    private static final Logger LOG = LoggerFactory.getLogger(NewJobPullerMessageBroker.class);

    @Autowired
    private Poller poller;

    /**
     * @param pProjectName
     *            the project name
     * @return a new jobInfo id
     * @throws RabbitMQVhostException
     *             message broker exceptions
     */
    @Override
    public Long getJob(final String pProjectName) {
        Long jobInfoId = null;
        final AmqpCommunicationMode pAmqpCommunicationMode = AmqpCommunicationMode.ONE_TO_ONE;
        final AmqpCommunicationTarget pAmqpCommunicationTarget = AmqpCommunicationTarget.INTERNAL;
        TenantWrapper<NewJobEvent> tenantWrapper;
        try {
            tenantWrapper = poller.poll(pProjectName, NewJobEvent.class, pAmqpCommunicationMode,
                                        pAmqpCommunicationTarget);
            final NewJobEvent newJobEvent = tenantWrapper.getContent();
            jobInfoId = newJobEvent.getJobId();
        } catch (final RabbitMQVhostException e) {
            LOG.error(String.format("Failed to fetch a jobInfo for tenant [%s]", pProjectName), e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return jobInfoId;
    }

}
