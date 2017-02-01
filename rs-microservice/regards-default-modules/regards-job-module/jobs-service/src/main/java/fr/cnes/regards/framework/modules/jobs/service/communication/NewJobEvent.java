/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.communication;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Léo Mieulet
 */
@Event(target = Target.MICROSERVICE)
public class NewJobEvent implements IPollable {

    /**
     * the jobInfo id
     */
    private long jobInfoId;

    /**
     * @param pJobInfoId
     *            the jobInfo id
     */
    public NewJobEvent(final long pJobInfoId) {
        super();
        jobInfoId = pJobInfoId;
    }

    /**
     * @return the jobId
     */
    public long getJobInfoId() {
        return jobInfoId;
    }

    /**
     * @param pJobId
     *            the jobId to set
     */
    public void setJobInfoId(final long pJobId) {
        jobInfoId = pJobId;
    }

}
