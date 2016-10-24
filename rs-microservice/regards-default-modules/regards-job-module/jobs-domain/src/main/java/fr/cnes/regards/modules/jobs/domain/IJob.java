/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public interface IJob extends Runnable {

    int getPriority();

    List<Output> getResults();

    StatusInfo getStatus();

    boolean hasResult();

    boolean needWorkspace();

    void setWorkspace(Path path);

    /**
     * @param pQueueEvent
     */
    void setQueueEvent(final BlockingQueue<IEvent> pQueueEvent);

    /**
     * @param pJobInfoId
     */
    void setJobInfoId(final Long pJobInfoId);

    /**
     * @param pParameters
     */
    void setParameters(JobParameters pParameters);

}
