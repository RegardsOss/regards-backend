package fr.cnes.regards.framework.microservice.manager;

import java.time.OffsetDateTime;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class MaintenanceInfo {

    /**
     * is maintenance mode active?
     */
    private Boolean active;

    /**
     * When was the last time it was enabled or disabled?
     */
    private OffsetDateTime lastUpdate;

    /**
     * Constructor setting whether the maintenance mode is active or not and the last update date
     */
    public MaintenanceInfo(Boolean active, OffsetDateTime lastUpdate) {
        this.active = active;
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return whether the maintenance mode is active or not
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Set whether the maintenace mode is active or not
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * @return the last update date
     */
    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Set the last update date
     */
    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
