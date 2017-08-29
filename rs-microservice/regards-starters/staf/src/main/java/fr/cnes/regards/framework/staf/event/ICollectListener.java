package fr.cnes.regards.framework.staf.event;

/**
 * The listener interface for receiving a data object collect evnet.
 *
 * @since 4.4
 */
public interface ICollectListener {

    /**
     * Invoked when a step has finished
     *
     * @since 4.4
     */
    void collectEnded(CollectEvent pEvent);
}
