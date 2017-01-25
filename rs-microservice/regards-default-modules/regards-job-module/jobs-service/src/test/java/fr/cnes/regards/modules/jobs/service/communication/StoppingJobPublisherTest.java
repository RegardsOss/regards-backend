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

/**
 *
 */

public class StoppingJobPublisherTest {

    private IPublisher publisherMock;

    private StoppingJobPublisher stoppingJobPublisher;

    private long jobInfoId;

    @Before
    public void setUp() {
        publisherMock = Mockito.mock(IPublisher.class);
        stoppingJobPublisher = new StoppingJobPublisher(publisherMock);
        jobInfoId = 5L;
    }

    @Test
    public void testPublishStoppingJob() {
        // Create ArgumentCaptor to capture the argument value
        final ArgumentCaptor<StoppingJobEvent> argumentNewJobEvent = ArgumentCaptor.forClass(StoppingJobEvent.class);

        stoppingJobPublisher.send(jobInfoId);

        // check if the function have been called once, and save its attributes
        Mockito.verify(publisherMock).publish(argumentNewJobEvent.capture());

        // Asserts attributes
        Assertions.assertThat(argumentNewJobEvent.getValue().getJobInfoId()).isEqualTo(jobInfoId);
    }
}
