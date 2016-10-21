/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.jobs.domain.IEvent;

/**
 * A thread between JobHandler and Jobs that receives events and send them back to JobHandler
 */
public class JobMonitor implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(JobMonitor.class);

    /**
     * Thread safe queue
     */
    private final BlockingQueue<IEvent> queueEvent;

    private final IJobHandler jobHandler;

    private boolean isRunning;

    /**
     * @param pJobHandler
     *
     */
    public JobMonitor(final JobHandler pJobHandler) {
        super();
        queueEvent = new LinkedBlockingDeque<>();
        jobHandler = pJobHandler;
        isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                final IEvent event = queueEvent.take();
                jobHandler.onEvent(event);
            } catch (final InterruptedException e) {
                LOG.warn("JobMonitor interrupted, closing");
                Thread.currentThread().interrupt();
                isRunning = false;
            }
        }
    }

    /**
     * @return the queueEvent
     */
    public BlockingQueue<IEvent> getQueueEvent() {
        return queueEvent;
    }

}
