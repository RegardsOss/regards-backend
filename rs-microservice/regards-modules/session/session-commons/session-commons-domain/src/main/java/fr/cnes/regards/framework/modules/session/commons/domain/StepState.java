package fr.cnes.regards.framework.modules.session.commons.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Current state of the {@link SessionStep}
 *
 * @author Iliana Ghazali
 **/
@Embeddable
public class StepState {

    @Column(name = "errors")
    private long errors = 0L;

    @Column(name = "waiting")
    private long waiting = 0L;

    @Column(name = "running")
    private long running = 0L;

    public StepState() {
    }

    public StepState(long errors, long waiting, long running) {
        this.errors = errors;
        this.waiting = waiting;
        this.running = running;
    }

    public long getErrors() {
        return errors;
    }

    public void setErrors(long errors) {
        this.errors = errors;
    }

    public long getWaiting() {
        return waiting;
    }

    public void setWaiting(long waiting) {
        this.waiting = waiting;
    }

    public long getRunning() {
        return running;
    }

    public void setRunning(long running) {
        this.running = running;
    }
}