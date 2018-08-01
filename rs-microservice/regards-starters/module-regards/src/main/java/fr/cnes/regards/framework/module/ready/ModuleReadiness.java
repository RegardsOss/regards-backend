package fr.cnes.regards.framework.module.ready;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class ModuleReadiness {

    /**
     * Whether the module is ready or not
     */
    private boolean ready;

    /**
     * reasons why the module is not ready
     */
    private List<String> reasons;

    /**
     * Constructor setting the readiness of the module and the reasons why it is not
     * @param ready
     * @param reasons
     */
    public ModuleReadiness(boolean ready, List<String> reasons) {
        this.ready = ready;
        this.reasons = reasons;
    }

    /**
     * @return whether the module is ready
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Set the module readiness
     * @param ready
     */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    /**
     * @return reasons why the module is not ready
     */
    public List<String> getReasons() {
        return reasons;
    }

    /**
     * Set the reasons why the module is not ready
     * @param reasons
     */
    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
