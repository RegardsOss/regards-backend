/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.communication;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.service.manager.IJobHandler;

/**
 *
 */
public class StoppingJobSubscriberTest {

    private StoppingJobEvent stoppingJobEvent;

    private String projectName;

    private StoppingJobSubscriber newJobPuller;

    private IJobHandler jobHandlerMock;

    private long jobInfoIdExpected;

    @Before
    public void setUp() {
        jobHandlerMock = Mockito.mock(IJobHandler.class);
        newJobPuller = new StoppingJobSubscriber(jobHandlerMock);
        jobInfoIdExpected = 1L;
        stoppingJobEvent = new StoppingJobEvent(jobInfoIdExpected);
        projectName = "project1";
    }

    @Test
    public void testGetJob() {
        final JobStatusInfo statusInfo = new JobStatusInfo();
        statusInfo.setStatus(JobStatus.RUNNING);
        Mockito.when(jobHandlerMock.abort(jobInfoIdExpected)).thenReturn(statusInfo);
        final TenantWrapper<StoppingJobEvent> value = new TenantWrapper<>(stoppingJobEvent, projectName);
        newJobPuller.handle(value);
        Mockito.verify(jobHandlerMock).abort(jobInfoIdExpected);

    }

}
