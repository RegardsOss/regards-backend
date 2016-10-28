/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

/**
 *
 */
public class NewJobEvent {

    private long jobId;

    /**
     * @param pJobId
     */
    public NewJobEvent(final long pJobId) {
        super();
        jobId = pJobId;
    }

    /**
     * @return the jobId
     */
    public long getJobId() {
        return jobId;
    }

    /**
     * @param pJobInfoId
     *            the jobId to set
     */
    public void setJobId(final long pJobInfoId) {
        jobId = pJobInfoId;
    }

}
