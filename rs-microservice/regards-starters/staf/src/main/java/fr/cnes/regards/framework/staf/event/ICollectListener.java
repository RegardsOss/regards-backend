/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.event;

/**
 * The listener interface for receiving a data object collect event.
 * @author Sébastien Binda
 */
public interface ICollectListener {

    /**
     * Invoked when a STAF restore stream is finished.
     */
    void collectEnded(CollectEvent pEvent);
}
