package fr.cnes.regards.framework.microservice.actuator;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import fr.cnes.regards.framework.microservice.manager.MaintenanceInfo;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Health indicator allowing us to know when was the last shift between maintenance mode and standard mode
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class MaintenanceHealthIndicator extends AbstractHealthIndicator {

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public MaintenanceHealthIndicator(IRuntimeTenantResolver runtimeTenantResolver) {
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        MaintenanceInfo info = MaintenanceManager.getMaintenanceMap().get(runtimeTenantResolver.getTenant());
        builder.withDetail("lastUpdate", info.getLastUpdate());
        if (info.getActive()) {
            builder.outOfService();
        } else {
            builder.up();
        }
    }
}
