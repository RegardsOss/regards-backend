package fr.cnes.regards.modules.workermanager.amqp.events.out;

/**
 * WorkerManager responses
 *
 * @author Sébastien Binda
 */
public enum ResponseStatus {

    /**
     * Request has been skipped and will never be handled.
     */
    SKIPPED,

    /**
     * Request has been granted
     */
    GRANTED,

    /**
     * Request has been delayed and wil be handled as soon as a worker is available
     */
    DELAYED,

    /**
     * Request body content has been invalidated by the worker
     */
    INVALID_CONTENT,

    /**
     * Request has been handle in error by the worker
     */
    ERROR,

    /**
     * Request has been successfully handled by the worker
     */
    SUCCESS;
}
