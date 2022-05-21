package fr.cnes.regards.modules.dam.service.settings;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.dam.domain.settings.DamSettings;
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
public class DamSettingsService extends AbstractSettingService implements IDamSettingsService {

    private final ITenantResolver tenantsResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private DamSettingsService self;

    protected DamSettingsService(IDynamicTenantSettingService dynamicTenantSettingService,
                                 ITenantResolver tenantsResolver,
                                 IRuntimeTenantResolver runtimeTenantResolver,
                                 DamSettingsService damSettingsService) {
        super(dynamicTenantSettingService);
        this.tenantsResolver = tenantsResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.self = damSettingsService;
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
    public boolean isStoreFiles() {
        Boolean isStoreFiles = getValue(DamSettings.STORE_FILES);
        return isStoreFiles != null && isStoreFiles;
    }

    @Override
    public void setStoreFiles(Boolean isStoreFiles) throws EntityException {
        dynamicTenantSettingService.update(DamSettings.STORE_FILES, isStoreFiles);
    }

    @Override
    public String getStorageLocation() {
        return getValue(DamSettings.STORAGE_LOCATION);
    }

    @Override
    public void setStorageLocation(String location) throws EntityException {
        dynamicTenantSettingService.update(DamSettings.STORAGE_LOCATION, location);
    }

    @Override
    public String getStorageSubDirectory() {
        return getValue(DamSettings.STORAGE_SUBDIRECTORY);
    }

    @Override
    public void setStorageSubDirectory(String subDirectory) throws EntityException {
        dynamicTenantSettingService.update(DamSettings.STORAGE_SUBDIRECTORY, subDirectory);
    }

    @Override
    public long getIndexNumberOfShards() {
        return getValue(DamSettings.INDEX_NUMBER_OF_SHARDS);
    }

    @Override
    public void setIndexNumberOfShards(Long numberOfShards) throws EntityException {
        dynamicTenantSettingService.update(DamSettings.INDEX_NUMBER_OF_SHARDS, numberOfShards);
    }

    @Override
    public long getIndexNumberOfReplicas() {
        return getValue(DamSettings.INDEX_NUMBER_OF_REPLICAS);
    }

    @Override
    public void setIndexNumberOfReplicas(Long numberOfReplicas) throws EntityException {
        dynamicTenantSettingService.update(DamSettings.INDEX_NUMBER_OF_REPLICAS, numberOfReplicas);
    }

    @Override
    protected List<DynamicTenantSetting> getSettingList() {
        return DamSettings.SETTING_LIST;
    }

}
