package fr.cnes.regards.modules.order.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.order.domain.settings.OrderSettings;
import fr.cnes.regards.modules.order.domain.settings.UserOrderParameters;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class OrderSettingsService extends AbstractSettingService implements IOrderSettingsService {

    private final ITenantResolver tenantsResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private OrderSettingsService self;

    public OrderSettingsService(IDynamicTenantSettingService dynamicTenantSettingService,
                                ITenantResolver tenantsResolver,
                                IRuntimeTenantResolver runtimeTenantResolver,
                                OrderSettingsService orderSettingsService) {
        super(dynamicTenantSettingService);
        this.tenantsResolver = tenantsResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.self = orderSettingsService;
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationStartedEvent(ApplicationStartedEvent applicationStartedEvent) throws EntityException {
        for (String tenant : tenantsResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                self.init();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onTenantConnectionReady(TenantConnectionReady event) throws EntityException {
        runtimeTenantResolver.forceTenant(event.getTenant());
        try {
            self.init();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        return OrderSettings.SETTING_LIST;
    }

    @Override
    public UserOrderParameters getUserOrderParameters() {
        return getValue(OrderSettings.USER_ORDER_PARAMETERS);
    }

    @Override
    public void setUserOrderParameters(UserOrderParameters userOrderParameters) throws EntityException {
        dynamicTenantSettingService.update(OrderSettings.USER_ORDER_PARAMETERS, userOrderParameters);
    }

    @Override
    public Integer getAppSubOrderDuration() {
        return getValue(OrderSettings.APP_SUB_ORDER_DURATION);
    }

    @Override
    public void setAppSubOrderDuration(int appSubOrderDuration) throws EntityException {
        dynamicTenantSettingService.update(OrderSettings.APP_SUB_ORDER_DURATION, appSubOrderDuration);
    }

}
