/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

/**
 * @author lmieulet
 */
public class CoupleThreadTenantName {

    /**
     * Store the running job
     */
    private final Thread thread;

    /**
     * Store the tenant name for this job
     */
    private final String tenantName;

    /**
     *
     * @param pTenantName
     *            the tenant name
     * @param pThread
     *            the running job
     */
    public CoupleThreadTenantName(final String pTenantName, final Thread pThread) {
        thread = pThread;
        tenantName = pTenantName;
    }

    /**
     * @return the thread
     */
    public Thread getThread() {
        return thread;
    }

    /**
     * @return the tenantName
     */
    public String getTenantName() {
        return tenantName;
    }

}