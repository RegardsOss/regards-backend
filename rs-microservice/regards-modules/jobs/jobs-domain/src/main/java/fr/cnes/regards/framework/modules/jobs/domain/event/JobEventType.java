package fr.cnes.regards.framework.modules.jobs.domain.event;

/**
 * Type of event that occured on a job.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public enum JobEventType {

    ABORTED(true),

    FAILED(true),

    RUNNING(false),

    SUCCEEDED(true);

    private final boolean finalState;

    JobEventType(boolean finalState) {
        this.finalState = finalState;
    }

    public boolean isFinalState() {
        return finalState;
    }

}
