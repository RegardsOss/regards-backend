package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.database.MonitoringAggregation;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugin.datastorage.PluginStorageInfo;

/**
 * Data storage service
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class DataStorageService implements IDataStorageService, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataStorageInfo.class);

    /**
     * {@link IPluginService} instance
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * {@link IDataFileDao} instance
     */
    @Autowired
    private IDataFileDao dataFileDao;

    /**
     * {@link ITenantResolver} instance
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * {@link INotificationClient} instance
     */
    @Autowired
    private INotificationClient notificationClient;

    /**
     * Spring application name ~= microservice type
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * data storage occupation threshold in percent
     */
    @Value("${regards.storage.data.storage.threshold.percent:90}")
    private Integer threshold;

    @Override
    public Collection<PluginStorageInfo> getMonitoringInfos() throws ModuleException, IOException {
        Set<PluginStorageInfo> monitoringInfos = Sets.newHashSet();
        List<PluginConfiguration> dataStorageConfigurations = pluginService
                .getPluginConfigurationsByType(IDataStorage.class);
        //lets take only the activated ones
        Set<PluginConfiguration> activeDataStorageConfs = dataStorageConfigurations.stream().filter(pc -> pc.isActive())
                .collect(Collectors.toSet());
        //for each active conf, lets get their DataStorageInfo
        // lets ask the data base to calculate the used space per data storage
        Collection<MonitoringAggregation> monitoringAggregations = dataFileDao.getMonitoringAggregation();
        // now lets transform it into Map<Long, Long>, it is easier to use
        Map<Long, Long> monitoringAggregationMap = monitoringAggregations.stream().collect(Collectors
                .toMap(MonitoringAggregation::getDataStorageUsedId, MonitoringAggregation::getUsedSize));
        for (PluginConfiguration activeDataStorageConf : activeDataStorageConfs) {
            //lets initialize the monitoring information for this data storage configuration by getting plugin informations
            Long activeDataStorageConfId = activeDataStorageConf.getId();
            PluginMetaData activeDataStorageMeta = pluginService
                    .getPluginMetaDataById(activeDataStorageConf.getPluginId());
            PluginStorageInfo monitoringInfo = new PluginStorageInfo(activeDataStorageConfId,
                    activeDataStorageMeta.getDescription(), activeDataStorageConf.getLabel());
            //now lets get the data storage monitoring information from the plugin
            Long dataStorageTotalSpace = ((IDataStorage) pluginService.getPlugin(activeDataStorageConfId))
                    .getTotalSpace();
            DataStorageInfo dataStorageInfo;
            if (monitoringAggregationMap.containsKey(activeDataStorageConfId)) {
                dataStorageInfo = new DataStorageInfo(activeDataStorageConfId.toString(), dataStorageTotalSpace,
                        monitoringAggregationMap.get(activeDataStorageConfId));
            } else {
                dataStorageInfo = new DataStorageInfo(activeDataStorageConfId.toString(), dataStorageTotalSpace, 0);

            }
            monitoringInfo.setTotalSize(dataStorageInfo.getTotalSize());
            monitoringInfo.setUsedSize(dataStorageInfo.getUsedSize());
            monitoringInfo.setRatio(dataStorageInfo.getRatio());

            monitoringInfos.add(monitoringInfo);
        }
        return monitoringInfos;
    }

    @Override
    @Scheduled(fixedRateString = "${regards.storage.check.data.storage.disk.usage.rate:60000}",
            initialDelay = 60 * 1000)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void monitorDataStorages() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            List<PluginConfiguration> dataStorageConfigurations = pluginService
                    .getPluginConfigurationsByType(IDataStorage.class);
            //lets take only the activated ones
            Set<PluginConfiguration> activeDataStorageConfs = dataStorageConfigurations.stream()
                    .filter(pc -> pc.isActive()).collect(Collectors.toSet());
            // lets ask the data base to calculate the used space per data storage
            Collection<MonitoringAggregation> monitoringAggregations = dataFileDao.getMonitoringAggregation();
            if (!monitoringAggregations.isEmpty()) {
                // now lets transform it into Map<Long, Long>, it is easier to use
                Map<Long, Long> monitoringAggregationMap = monitoringAggregations.stream().collect(Collectors
                        .toMap(MonitoringAggregation::getDataStorageUsedId, MonitoringAggregation::getUsedSize));
                // lets instantiate those data storage and get their total space
                for (PluginConfiguration activeDataStorageConf : activeDataStorageConfs) {
                    //lets initialize the monitoring information for this data storage configuration by getting plugin informations
                    try {
                        IDataStorage<?> activeDataStorage = pluginService.getPlugin(activeDataStorageConf.getId());

                        Long activeDataStorageConfId = activeDataStorageConf.getId();
                        Long dataStorageTotalSpace = activeDataStorage.getTotalSpace();
                        if (monitoringAggregationMap.containsKey(activeDataStorageConfId)) {
                            DataStorageInfo dataStorageInfo = new DataStorageInfo(activeDataStorageConfId.toString(),
                                    dataStorageTotalSpace, monitoringAggregationMap.get(activeDataStorageConfId));
                            Double ratio = dataStorageInfo.getRatio();
                            if (ratio >= threshold) {
                                String message = String.format(
                                                               "Data storage(configuration id: %s, configuration label: %s) has reach its disk usage threshold. Actual occupation: %s, threshold: %s",
                                                               activeDataStorageConf.getId().toString(),
                                                               activeDataStorageConf.getLabel(), ratio, threshold);
                                LOG.error(message);
                                notifyAdmins("Data storage " + activeDataStorageConf.getLabel() + " is almost full",
                                             message, NotificationType.ERROR);
                                MaintenanceManager.setMaintenance(tenant);
                            }

                        }

                    } catch (ModuleException e) {
                        //should never happens, currently it is an exception that cannot be thrown in this code(issues with dynamic parameters)
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        pluginService.addPluginPackage("fr.cnes.regards.modules.storage");
    }

    /**
     * Use the notification module in admin to create a notification for admins
     */
    private void notifyAdmins(String title, String message, NotificationType type) {
        NotificationDTO notif = new NotificationDTO(message, Sets.newHashSet(),
                Sets.newHashSet(DefaultRole.ADMIN.name()), applicationName, title, type);
        FeignSecurityManager.asSystem();
        notificationClient.createNotification(notif);
        FeignSecurityManager.reset();
    }
}
