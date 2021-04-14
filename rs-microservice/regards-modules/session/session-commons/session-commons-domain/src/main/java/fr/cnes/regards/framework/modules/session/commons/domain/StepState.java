package fr.cnes.regards.framework.modules.session.commons.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Iliana Ghazali
 **/
@Embeddable
public class StepState {

    @Column(name="errors")
    private Long errors = 0L;

    @Column(name="waiting")
    private Long waiting = 0L;

    @Column(name="running")
    private boolean running = false;

    public Long getErrors() {
        return errors;
    }

    public void setErrors(Long errors) {
        this.errors = errors;
    }

    public Long getWaiting() {
        return waiting;
    }

    public void setWaiting(Long waiting) {
        this.waiting = waiting;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
