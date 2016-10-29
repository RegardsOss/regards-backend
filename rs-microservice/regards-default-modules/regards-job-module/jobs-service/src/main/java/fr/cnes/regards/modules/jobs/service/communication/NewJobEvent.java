/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

/**
 *
 */
public class NewJobEvent {

    private long jobInfoId;

    /**
     * @param pJobId
     */
    public NewJobEvent(final long pJobId) {
        super();
        jobInfoId = pJobId;
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
