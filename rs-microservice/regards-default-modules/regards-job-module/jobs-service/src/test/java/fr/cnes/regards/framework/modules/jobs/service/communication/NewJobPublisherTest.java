/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.communication;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.modules.jobs.service.communication.INewJobPublisher;
import fr.cnes.regards.framework.modules.jobs.service.communication.NewJobEvent;
import fr.cnes.regards.framework.modules.jobs.service.communication.NewJobPublisher;

/**
 * @author Léo Mieulet
 */

public class NewJobPublisherTest {

    private IPublisher publisherMock;

    private INewJobPublisher newJobPublisherMessageBroker;

    private AmqpCommunicationTarget target;

    private AmqpCommunicationMode mode;

    private NewJobEvent newJobEvent;

    private long jobInfoId;

    @Before
    public void setUp() {
        publisherMock = Mockito.mock(IPublisher.class);
        newJobPublisherMessageBroker = new NewJobPublisher(publisherMock);
        mode = AmqpCommunicationMode.ONE_TO_ONE;
        target = AmqpCommunicationTarget.MICROSERVICE;
        jobInfoId = 5L;
        newJobEvent = new NewJobEvent(jobInfoId);
    }

    @Test
    public void testPublishJob() throws RabbitMQVhostException {
        // Create ArgumentCaptor to capture the argument value
        final ArgumentCaptor<NewJobEvent> argumentNewJobEvent = ArgumentCaptor.forClass(NewJobEvent.class);
        final ArgumentCaptor<String> argumentString = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<AmqpCommunicationTarget> argumentCommunicationTarget = ArgumentCaptor
                .forClass(AmqpCommunicationTarget.class);
        final ArgumentCaptor<AmqpCommunicationMode> argumentCommunicationMode = ArgumentCaptor
                .forClass(AmqpCommunicationMode.class);

        newJobPublisherMessageBroker.sendJob(jobInfoId);

        // check if the function have been called once, and save its attributes
        Mockito.verify(publisherMock).publish(argumentNewJobEvent.capture(), argumentCommunicationMode.capture(),
                                              argumentCommunicationTarget.capture());

        // Asserts attributes
        Assertions.assertThat(argumentNewJobEvent.getValue().getJobInfoId()).isEqualTo(jobInfoId);
        Assertions.assertThat(argumentCommunicationTarget.getValue()).isEqualTo(target);
        Assertions.assertThat(argumentCommunicationMode.getValue()).isEqualTo(mode);
    }
}
