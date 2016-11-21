/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

/**
 * The JobAllocationStrategy plugin returns JobQueue to allocate a number of slot per tenant
 * 
 * @author Léo Mieulet
 */
public interface IJobQueue {

    public String getName();

    public int getCurrentSize();

    public int getMaxSize();

    /**
     * @param pCurrentSize
     *            the currentSize to set
     */
    void setCurrentSize(int pCurrentSize);

}
