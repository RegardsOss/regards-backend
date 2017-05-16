package fr.cnes.regards.framework.microservice.manager;

import java.time.OffsetDateTime;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class MaintenanceInfo{

    /**
     * is maintenance mode active?
     */
    private Boolean active;

    /**
     * When was the last time it was enabled or disabled?
     */
    private OffsetDateTime lastUpdate;

    public MaintenanceInfo(Boolean active, OffsetDateTime lastUpdate) {
        this.active = active;
        this.lastUpdate = lastUpdate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
