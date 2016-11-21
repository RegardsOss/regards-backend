/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

/**
 * @author Léo Mieulet
 */
public class NewJobEvent {

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
