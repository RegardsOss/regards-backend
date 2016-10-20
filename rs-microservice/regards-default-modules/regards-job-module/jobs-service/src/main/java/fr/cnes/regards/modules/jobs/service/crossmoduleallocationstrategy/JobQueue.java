/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

import fr.cnes.regards.modules.jobs.service.manager.IEvent;

public class JobQueue implements IJobQueue {

    private final String name;

    private final int currentSize;

    private final int maxSize;

    /**
     *
     * @param pName
     * @param pCurrentSize
     * @param pMaxSize
     */
    public JobQueue(String pName, int pCurrentSize, int pMaxSize) {
        super();
        name = pName;
        currentSize = pCurrentSize;
        maxSize = pMaxSize;
    }

    @Override
    public IEvent get() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void publish(IEvent pEvent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void subscribe(IEvent pEvent) {
        // TODO Auto-generated method stub

    }

    /**
     * @return the name
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

}
