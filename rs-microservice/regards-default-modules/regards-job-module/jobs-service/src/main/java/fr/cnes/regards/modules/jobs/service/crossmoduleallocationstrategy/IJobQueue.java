/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

import fr.cnes.regards.modules.jobs.service.manager.IEvent;

public interface IJobQueue {

    public IEvent get();

    public void publish(IEvent pEvent);

    public void subscribe(IEvent pEvent);

    public String getName();

    public int getCurrentSize();

    public int getMaxSize();

}
