/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 */
public class NewJobPullerTest {

    private INewJobPuller newJobPullerMessageBroker;

    private Poller pollerMock;

    private NewJobEvent newJobEvent;

    private String projectName;

    @Before
    public void setUp() {
        pollerMock = Mockito.mock(Poller.class);
        pollerMock.toString();
        newJobPullerMessageBroker = new NewJobPuller(pollerMock);
        newJobEvent = new NewJobEvent(1L);
        projectName = "project1";
    }

    @Test
    public void testGetJob() throws RabbitMQVhostException {
        final long jobInfoIdExpected = 666L;
        // Also test the setter
        newJobEvent.setJobInfoId(jobInfoIdExpected);
        final TenantWrapper<NewJobEvent> value = new TenantWrapper<>(newJobEvent, projectName);
        Mockito.when(pollerMock.poll(projectName, NewJobEvent.class)).thenReturn(value);
        final Long jobInfoId = newJobPullerMessageBroker.getJob(projectName);
        Assertions.assertThat(jobInfoId).isEqualTo(jobInfoIdExpected);
    }

    @Test
    public void testGetJobWhenRabbitException() throws RabbitMQVhostException {
        Mockito.doThrow(new RabbitMQVhostException("some exception")).when(pollerMock).poll(projectName,
                                                                                            newJobEvent.getClass());

        final Long jobInfoId = newJobPullerMessageBroker.getJob(projectName);
        Assertions.assertThat(jobInfoId).isNull();

    }
}
