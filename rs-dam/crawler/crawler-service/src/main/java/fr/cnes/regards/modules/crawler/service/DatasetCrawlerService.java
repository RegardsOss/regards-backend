package fr.cnes.regards.modules.crawler.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.event.AccessRightEvent;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.event.AccessRightEventType;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.event.DatasetEvent;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.gson.ModelJsonReadyEvent;
import fr.cnes.regards.modules.model.service.event.ComputedAttributeModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Crawler service for Dataset. <b>This service need @EnableSchedule at Configuration</b>
 *
 * @author oroussel
 */
@Service
public class DatasetCrawlerService extends AbstractCrawlerService<DatasetEvent>
    implements IDatasetCrawlerService, IHandler<AccessRightEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetCrawlerService.class);

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IDatasetService datasetService;

    @Autowired
    private IDatasetRepository datasetRepository;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    /**
     * Self proxy
     */
    @Autowired
    @Lazy
    private IDatasetCrawlerService self;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    @EventListener
    public void onApplicationReadyEvent(ModelJsonReadyEvent event) {
        subscriber.subscribeTo(AccessRightEvent.class, this);
    }

    @Override
    @EventListener
    @RegardsTransactional
    public void onComputedAttributeModelEvent(ComputedAttributeModelEvent event) {
        ModelAttrAssoc modelAttrAssoc = event.getSource();
        // Only recompute if a plugin conf is set (a priori if a plugin confis removed it is to be changed soon)
        if (modelAttrAssoc.getComputationConf() != null) {
            Set<Dataset> datasets = datasetService.findAllByModel(modelAttrAssoc.getModel().getId());
            for (Dataset dataset : datasets) {
                try {
                    datasetRepository.save(dataset);
                    entityIndexerService.updateEntityIntoEs(tenantResolver.getTenant(),
                                                            dataset.getIpId(),
                                                            OffsetDateTime.now(),
                                                            true);
                } catch (ModuleException e) {
                    LOGGER.error("Cannot update dataset", e);
                }
            }
        }
    }

    @Override
    @Async(CrawlerTaskExecutorConfiguration.CRAWLER_EXECUTOR_BEAN)
    public void crawl() {
        super.crawl(self::doPoll);
    }

    @Override
    public void handle(TenantWrapper<AccessRightEvent> wrapper) {
        if (wrapper.getContent() != null) {
            AccessRightEvent event = wrapper.getContent();
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                notifyAccessRightUpdateBeginning(event);
                entityIndexerService.updateEntityIntoEs(wrapper.getTenant(),
                                                        event.getDatasetIpId(),
                                                        OffsetDateTime.now(),
                                                        true);
                notifyAccessRightUpdateDone(event);
            } catch (ModuleException e) {
                LOGGER.error("Cannot handle access right event", e);
                notifyAccessRightUpdateError(event, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    private void notifyAccessRightUpdateError(AccessRightEvent accessRightEvent, ModuleException e) {
        if (accessRightEvent.getEventType() == AccessRightEventType.DELETE) {
            notificationClient.notifyRoles(String.format(
                                               "Dataset %s access right could not be removed for users having Group %s because of an unexpected issue: \"%s\"."
                                               + " Please try to remove them once again.",
                                               accessRightEvent.getDatasetLabel(),
                                               accessRightEvent.getAccessGroupName(),
                                               e.getMessage()),
                                           "Access right removal error",
                                           NotificationLevel.ERROR,
                                           MimeTypeUtils.TEXT_PLAIN,
                                           Sets.newHashSet(accessRightEvent.getRoleToNotify()));
        } else {
            String message = String.format(
                "Dataset %s access rights could not be modified after all because of an unexpected issue: \"%s\"."
                + " Please change them once again to retry.",
                accessRightEvent.getDatasetLabel(),
                e.getMessage());
            notificationClient.notifyRoles(message,
                                           "Access right update error",
                                           NotificationLevel.ERROR,
                                           MimeTypeUtils.TEXT_PLAIN,
                                           Sets.newHashSet(accessRightEvent.getRoleToNotify()));
        }
    }

    private void notifyAccessRightUpdateBeginning(AccessRightEvent accessRightEvent) {
        if (accessRightEvent.getEventType() == AccessRightEventType.DELETE) {
            notificationClient.notifyRoles(String.format(
                                               "Some access right will be removed between the dataset %s and the group %s",
                                               accessRightEvent.getDatasetLabel(),
                                               accessRightEvent.getAccessGroupName()),
                                           "Access right removal beginning",
                                           NotificationLevel.INFO,
                                           MimeTypeUtils.TEXT_PLAIN,
                                           Sets.newHashSet(accessRightEvent.getRoleToNotify()));
        } else {
            // Dataset {} access rights are being modified. Users having group {} ....
            String message;
            switch (accessRightEvent.getAccessLevel()) {
                case FULL_ACCESS:
                    message = String.format("Dataset %s access rights are being modified."
                                            + " Users having group %s have access to this dataset metadata and its data metadata."
                                            + " Access to physical data is: %s",
                                            accessRightEvent.getDatasetLabel(),
                                            accessRightEvent.getAccessGroupName(),
                                            accessRightEvent.getDataAccessLevel());
                    break;
                case RESTRICTED_ACCESS:
                    message = String.format("Dataset %s access rights are being modified."
                                            + " Users having group %s have access to this dataset."
                                            + " This means they can only see its metadata and no information about its data.",
                                            accessRightEvent.getDatasetLabel(),
                                            accessRightEvent.getAccessGroupName());
                    break;
                case CUSTOM_ACCESS:
                    message = String.format("Dataset %s access rights are being modified."
                                            + " Users having group %s have access to this dataset metadata"
                                            + " and its data access is decided by the plugin %s.",
                                            accessRightEvent.getDatasetLabel(),
                                            accessRightEvent.getAccessGroupName(),
                                            accessRightEvent.getDataAccessPluginLabel());
                    break;
                case NO_ACCESS:
                    message = String.format("Dataset %s access rights are being modified."
                                            + " Users having group %s has no access to this dataset metadata and its data.",
                                            accessRightEvent.getDatasetLabel(),
                                            accessRightEvent.getAccessGroupName());
                    break;
                default:
                    message = String.format(
                        "Dataset %s access rights are being modified with an undocumented access level %s.",
                        accessRightEvent.getDatasetLabel(),
                        accessRightEvent.getAccessLevel());
                    break;
            }
            notificationClient.notifyRoles(message,
                                           "Access right update beginning",
                                           NotificationLevel.INFO,
                                           MimeTypeUtils.TEXT_PLAIN,
                                           Sets.newHashSet(accessRightEvent.getRoleToNotify()));
        }
    }

    private void notifyAccessRightUpdateDone(AccessRightEvent accessRightEvent) {
        if (accessRightEvent.getEventType() == AccessRightEventType.DELETE) {
            notificationClient.notifyRoles(String.format(
                                               "Some access right has been removed between the dataset %s and the group %s",
                                               accessRightEvent.getDatasetLabel(),
                                               accessRightEvent.getAccessGroupName()),
                                           "Access right removal done",
                                           NotificationLevel.INFO,
                                           MimeTypeUtils.TEXT_PLAIN,
                                           Sets.newHashSet(accessRightEvent.getRoleToNotify()));
        } else {
            // Dataset {} access right has been modified. Users having group {} ....
            String message;
            switch (accessRightEvent.getAccessLevel()) {
                case FULL_ACCESS:
                    message = String.format("Dataset %s access right has been modified."
                                            + " Users having group %s have access to this dataset metadata and its data metadata."
                                            + " Access to physical data is: %s",
                                            accessRightEvent.getDatasetLabel(),
                                            accessRightEvent.getAccessGroupName(),
                                            accessRightEvent.getDataAccessLevel());
                    break;
                case RESTRICTED_ACCESS:
                    message = String.format("Dataset %s access right has been modified."
                                            + " Users having group %s have access to this dataset."
                                            + " This means they can only see its metadata and no information about its data.",
                                            accessRightEvent.getDatasetLabel(),
                                            accessRightEvent.getAccessGroupName());
                    break;
                case CUSTOM_ACCESS:
                    message = String.format("Dataset %s access right has been modified."
                                            + " Users having group %s have access to this dataset metadata"
                                            + " and its data access is decided by the plugin %s.",
                                            accessRightEvent.getDatasetLabel(),
                                            accessRightEvent.getAccessGroupName(),
                                            accessRightEvent.getDataAccessPluginLabel());
                    break;
                case NO_ACCESS:
                    message = String.format("Dataset %s access right has been modified."
                                            + " Users having group %s has no access to this dataset metadata and its data.",
                                            accessRightEvent.getDatasetLabel(),
                                            accessRightEvent.getAccessGroupName());
                    break;
                default:
                    message = String.format(
                        "Dataset %s access rights have been modified with an undocumented access level %s.",
                        accessRightEvent.getDatasetLabel(),
                        accessRightEvent.getAccessLevel());
                    break;
            }
            notificationClient.notifyRoles(message,
                                           "Access right update done",
                                           NotificationLevel.INFO,
                                           MimeTypeUtils.TEXT_PLAIN,
                                           Sets.newHashSet(accessRightEvent.getRoleToNotify()));
        }
    }

}
