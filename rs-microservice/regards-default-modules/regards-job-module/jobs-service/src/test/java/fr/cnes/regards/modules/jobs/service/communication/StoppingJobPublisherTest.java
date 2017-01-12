/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 */

public class StoppingJobPublisherTest {

    private IPublisher publisherMock;

    private StoppingJobPublisher stoppingJobPublisher;

    private String projectName;

    private AmqpCommunicationTarget target;

    private AmqpCommunicationMode mode;

    private StoppingJobEvent stoppingJobEvent;

    private long jobInfoId;

    @Before
    public void setUp() {
        publisherMock = Mockito.mock(IPublisher.class);
        stoppingJobPublisher = new StoppingJobPublisher(publisherMock);
        mode = AmqpCommunicationMode.ONE_TO_MANY;
        target = AmqpCommunicationTarget.MICROSERVICE;
        jobInfoId = 5L;
        stoppingJobEvent = new StoppingJobEvent(jobInfoId);
    }

    @Test
    public void testPublishStoppingJob() throws RabbitMQVhostException {
        // Create ArgumentCaptor to capture the argument value
        final ArgumentCaptor<StoppingJobEvent> argumentNewJobEvent = ArgumentCaptor.forClass(StoppingJobEvent.class);
        final ArgumentCaptor<String> argumentString = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<AmqpCommunicationTarget> argumentCommunicationTarget = ArgumentCaptor
                .forClass(AmqpCommunicationTarget.class);
        final ArgumentCaptor<AmqpCommunicationMode> argumentCommunicationMode = ArgumentCaptor
                .forClass(AmqpCommunicationMode.class);

        stoppingJobPublisher.send(jobInfoId);

        // check if the function have been called once, and save its attributes
        Mockito.verify(publisherMock).publish(argumentNewJobEvent.capture(), argumentCommunicationMode.capture(),
                                              argumentCommunicationTarget.capture());

        // Asserts attributes
        Assertions.assertThat(argumentNewJobEvent.getValue().getJobInfoId()).isEqualTo(jobInfoId);
        Assertions.assertThat(argumentCommunicationTarget.getValue()).isEqualTo(target);
        Assertions.assertThat(argumentCommunicationMode.getValue()).isEqualTo(mode);
    }
}
