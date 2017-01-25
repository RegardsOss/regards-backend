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
 * @author Léo Mieulet
 */

public class NewJobPublisherTest {

    private IPublisher publisherMock;

    private INewJobPublisher newJobPublisherMessageBroker;

    private long jobInfoId;

    @Before
    public void setUp() {
        publisherMock = Mockito.mock(IPublisher.class);
        newJobPublisherMessageBroker = new NewJobPublisher(publisherMock);
        jobInfoId = 5L;
    }

    @Test
    public void testPublishJob() {
        // Create ArgumentCaptor to capture the argument value
        final ArgumentCaptor<NewJobEvent> argumentNewJobEvent = ArgumentCaptor.forClass(NewJobEvent.class);

        newJobPublisherMessageBroker.sendJob(jobInfoId);

        // check if the function have been called once, and save its attributes
        Mockito.verify(publisherMock).publish(argumentNewJobEvent.capture());

        // Asserts attributes
        Assertions.assertThat(argumentNewJobEvent.getValue().getJobInfoId()).isEqualTo(jobInfoId);
    }
}
