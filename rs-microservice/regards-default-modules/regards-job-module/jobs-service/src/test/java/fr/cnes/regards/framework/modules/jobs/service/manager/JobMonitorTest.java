/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.manager;

import java.util.concurrent.BlockingQueue;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import fr.cnes.regards.framework.modules.jobs.domain.IEvent;
import fr.cnes.regards.framework.modules.jobs.service.manager.JobHandler;
import fr.cnes.regards.framework.modules.jobs.service.manager.JobMonitor;

/**
 *
 */
public class JobMonitorTest {

    private JobMonitor jobMonitor;

    private JobHandler jobHandler;

    @Test
    public void testRunWhenException() throws InterruptedException {
        jobHandler = Mockito.mock(JobHandler.class);
        jobMonitor = new JobMonitor(jobHandler);
        final BlockingQueue<IEvent> queueMock = Mockito.mock(BlockingQueue.class);
        ReflectionTestUtils.setField(jobMonitor, "queueEvent", queueMock, BlockingQueue.class);

        Mockito.doThrow(new InterruptedException("some exception")).when(queueMock).take();

        jobMonitor.run();
        Mockito.verifyZeroInteractions(jobHandler);

    }
}
