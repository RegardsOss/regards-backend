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
public class StoppingJobEvent implements IPollable {

    private final Long jobInfoId;

    /**
     * @param pJobInfoId
     *            the jobInfo id
     */
    public StoppingJobEvent(final Long pJobInfoId) {
        jobInfoId = pJobInfoId;
    }

    /**
     * @return the jobInfoId
     */
    public Long getJobInfoId() {
        return jobInfoId;
    }

}
