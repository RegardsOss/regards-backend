package fr.cnes.regards.framework.module.ready;

import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class ModuleReadiness {

    private boolean ready;

    private List<String> reasons;

    public ModuleReadiness(boolean ready, List<String> reasons) {
        this.ready = ready;
        this.reasons = reasons;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
