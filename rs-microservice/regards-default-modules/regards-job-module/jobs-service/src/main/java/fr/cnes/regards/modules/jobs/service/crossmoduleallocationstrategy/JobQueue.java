/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

/**
 * Store tenant/number of active jobs/max number of jobs
 * 
 * @author Léo Mieulet
 */
public class JobQueue implements IJobQueue {

    /**
     * the tenant name
     */
    private final String name;

    /**
     * Number of current working job for that tenant
     */
    private int currentSize;

    /**
     * JobAllocationStrategy maintain the maximal number of thread
     */
    private final int maxSize;

    /**
     *
     * @param pName
     *            tenant name
     * @param pCurrentSize
     *            number of working job for that tenant
     * @param pMaxSize
     *            max number of job for that tenant
     */
    public JobQueue(final String pName, final int pCurrentSize, final int pMaxSize) {
        super();
        name = pName;
        currentSize = pCurrentSize;
        maxSize = pMaxSize;
    }

    /**
     * @return the tenant name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return the currentSize
     */
    @Override
    public int getCurrentSize() {
        return currentSize;
    }

    /**
     * @return the maxSize
     */
    @Override
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * @param pCurrentSize
     *            the currentSize to set
     */
    @Override
    public void setCurrentSize(final int pCurrentSize) {
        currentSize = pCurrentSize;
    }

}
