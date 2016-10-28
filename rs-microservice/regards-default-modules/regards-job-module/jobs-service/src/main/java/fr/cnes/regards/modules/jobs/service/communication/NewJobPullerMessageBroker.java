/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 */
public class NewJobPullerMessageBroker {

    @Autowired
    private Poller poller;

    /**
     * @param pProjectName
     *            the project name
     * @return a new jobInfo id
     * @throws RabbitMQVhostException
     *             message broker exceptions
     */
    public Long getJob(final String pProjectName) throws RabbitMQVhostException {
        final AmqpCommunicationMode pAmqpCommunicationMode = AmqpCommunicationMode.ONE_TO_ONE;
        final AmqpCommunicationTarget pAmqpCommunicationTarget = AmqpCommunicationTarget.INTERNAL;
        final TenantWrapper<NewJobEvent> tenantWrapper = poller.poll(pProjectName, NewJobEvent.class,
                                                                     pAmqpCommunicationMode, pAmqpCommunicationTarget);
        final NewJobEvent content = tenantWrapper.getContent();
        return null;
    }

}
