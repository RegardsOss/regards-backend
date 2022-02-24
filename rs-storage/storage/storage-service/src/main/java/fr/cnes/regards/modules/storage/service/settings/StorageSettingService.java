package fr.cnes.regards.modules.storage.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.List;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class StorageSettingService extends AbstractSettingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageSettingService.class);

    private ITenantResolver tenantsResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private StorageSettingService self;

    @Value("${regards.storage.cache.path:cache}")
    private String defaultCachePath;

    protected StorageSettingService(IDynamicTenantSettingService dynamicTenantSettingService,
                                    ITenantResolver tenantsResolver,
                                    IRuntimeTenantResolver runtimeTenantResolver,
                                    StorageSettingService storageSettingService) {
        super(dynamicTenantSettingService);
        this.tenantsResolver = tenantsResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        self = storageSettingService;
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        // Default tenant cache path has to be fully computed at runtime
        // we can happily set the value no matter what as it will only be applied once by initialization logic
        DynamicTenantSetting tenantCachePath = StorageSetting.CACHE_PATH;
        String tenant = runtimeTenantResolver.getTenant();
        tenantCachePath.setDefaultValue(Paths.get(defaultCachePath).resolve(tenant));
        // default value is only there as information for users, so we also need to set the correct value for the first initialization
        tenantCachePath.setValue(Paths.get(defaultCachePath).resolve(tenant));
        return StorageSetting.SETTING_LIST;
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationStartedEvent(ApplicationStartedEvent applicationStartedEvent) throws EntityException {
        for (String tenant : tenantsResolver.getAllActiveTenants()) {
            initialize(tenant);
        }
    }

    private void initialize(String tenant)
            throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.info("Initializing dynamic tenant settings for tenant {}", tenant);
        try {
            self.init();
        } finally {
            runtimeTenantResolver.clearTenant();
        }
        LOGGER.info("Dynamic tenant settings initialization done for tenant {}", tenant);
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onTenantConnectionReady(TenantConnectionReady event) throws EntityException {
        initialize(event.getTenant());
    }
}
