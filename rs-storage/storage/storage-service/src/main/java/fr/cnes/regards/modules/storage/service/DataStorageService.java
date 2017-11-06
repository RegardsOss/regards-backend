package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.plugin.datastorage.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.PluginStorageInfo;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class DataStorageService implements IDataStorageService, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(DataStorageInfo.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public Collection<PluginStorageInfo> getMonitoringInfos() throws ModuleException, IOException {
        Set<PluginStorageInfo> monitoringInfos = Sets.newHashSet();
        List<PluginConfiguration> dataStorageConfigurations = pluginService
                .getPluginConfigurationsByType(IDataStorage.class);
        //lets take only the activated ones
        Set<PluginConfiguration> activeDataStorageConfs = dataStorageConfigurations.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toSet());
        //for each active conf, lets get their DataStorageInfo
        for (PluginConfiguration activeDataStorageConf : activeDataStorageConfs) {
            //lets initialize the monitoring information for this data storage configuration by getting plugin informations
            PluginMetaData activeDataStorageMeta = pluginService
                    .getPluginMetaDataById(activeDataStorageConf.getPluginId());
            PluginStorageInfo monitoringInfo = new PluginStorageInfo(activeDataStorageConf.getId(),
                                                                     activeDataStorageMeta.getDescription(),
                                                                     activeDataStorageConf.getLabel());
            //now lets get the data storage monitoring information from the plugin
            monitoringInfo.getStorageInfo()
                    .addAll(((IDataStorage) pluginService.getPlugin(activeDataStorageConf.getId()))
                                    .getMonitoringInfos());

            monitoringInfos.add(monitoringInfo);
        }
        return monitoringInfos;
    }

    @Override
    @Scheduled(fixedRateString = "${regards.storage.check.data.storage.disk.usage.rate:60000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void monitorDataStorages() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            List<PluginConfiguration> dataStorageConfigurations = pluginService
                    .getPluginConfigurationsByType(IDataStorage.class);
            //lets take only the activated ones
            Set<PluginConfiguration> activeDataStorageConfs = dataStorageConfigurations.stream()
                    .filter(pc -> pc.isActive()).collect(Collectors.toSet());
            // lets instantiate those data storage and check if their disk usage is above their configured threshold
            for (PluginConfiguration activeDataStorageConf : activeDataStorageConfs) {
                //lets initialize the monitoring information for this data storage configuration by getting plugin informations
                try {
                    IDataStorage<?> activeDataStorage = pluginService.getPlugin(activeDataStorageConf.getId());

                    Set<DataStorageInfo> dataStorageInfos = activeDataStorage.getMonitoringInfos();
                    for (DataStorageInfo dataStorageInfo : dataStorageInfos) {
                        Double ratio = dataStorageInfo.getRatio();
                        Integer threshold = activeDataStorage.getDiskUsageThreshold();
                        if (ratio >= threshold) {
                            //TODO: notify
                            LOG.error(
                                    "Data storage(configuration id: {}, configuration label: {}) has reach its disk usage threshold. Actual occupation: {}, threshold: {}",
                                    activeDataStorageConf.getId().toString(), activeDataStorageConf.getLabel(), ratio,
                                    threshold);
                            MaintenanceManager.setMaintenance(tenant);
                        }
                    }
                } catch (ModuleException e) {
                    //should never happens, currently it is an exception that cannot be thrown in this code(issues with dynamic parameters)
                    LOG.error(e.getMessage(), e);
                } catch (IOException e) {
                    //we could not get monitoring information from the plugin
                    LOG.error(e.getMessage(), e);
                    //TODO notify. maintenance mode or desactivate the plugin??
                }
            }
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        pluginService.addPluginPackage(IDataStorage.class.getPackage().getName());
    }
}
