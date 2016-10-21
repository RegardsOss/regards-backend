/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

/**
 *
 */
public class CoupleThreadTenantName {

    public final Thread thread;

    public final String tenantName;

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