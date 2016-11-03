/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

/**
 * @author lmieulet
 */
public class StoppingJobEvent {

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
