/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

/**
 * The JobAllocationStrategy plugin returns JobQueue to allocate a number of slot per tenant
 */
public interface IJobQueue {

    public String getName();

    public int getCurrentSize();

    public int getMaxSize();

}
