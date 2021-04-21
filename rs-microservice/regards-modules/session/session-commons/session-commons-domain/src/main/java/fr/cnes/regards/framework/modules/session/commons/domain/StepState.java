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
    private long errors;

    @Column(name = "waiting")
    private long waiting;

    @Column(name = "running")
    private boolean running;

    public StepState() {
        this.errors = 0L;
        this.waiting = 0L;
        this.running = false;
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

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}