/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.workspace.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.modules.workspace.domain.WorkspaceMonitoringEvent;
import fr.cnes.regards.framework.modules.workspace.domain.WorkspaceMonitoringInformation;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Default {@link IWorkspaceService} implementation which dive the workspace per microservice and per tenant.
 *
 * @author svissier
 *
 */
@Service
@ConditionalOnMissingBean(value = IWorkspaceService.class)
public class WorkspaceService implements IWorkspaceService, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Workspace service logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * {@link IWorkspaceNotifier} instance
     */
    @Autowired
    private IWorkspaceNotifier notifier;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ISubscriber subscriber;

    /**
     * The workspace configured path. Default value is only useful for testing purpose.
     */
    @Value("${regards.workspace:target/workspace}")
    private String workspaceBasePath;

    /**
     * The workspace occupation threshold at which point notification should be sent
     */
    @Value("${regards.workspace.occupation.threshold:90}")
    private Integer workspaceOccupationThreshold;

    /**
     * the spring application name
     */
    @Value("${spring.application.name}")
    private String springApplicationName;

    @Override
    public void setIntoWorkspace(InputStream is, String fileName) throws IOException {
        Path workspacePath = getMicroserviceWorkspace();
        if (Files.notExists(workspacePath)) {
            Files.createDirectories(workspacePath);
        }
        OutputStream os = Files.newOutputStream(getFilePath(fileName), StandardOpenOption.CREATE);
        ByteStreams.copy(is, os);
        os.flush();
        os.close();
    }

    @Override
    public InputStream retrieveFromWorkspace(String fileName) throws IOException {
        return Files.newInputStream(getFilePath(fileName));
    }

    @Override
    public void removeFromWorkspace(String fileName) throws IOException {
        Files.deleteIfExists(getFilePath(fileName));
    }

    @Override
    public Path getPrivateDirectory() throws IOException {
        Path privateDir = Paths.get(getMicroserviceWorkspace().toString(), UUID.randomUUID().toString());
        Files.createDirectories(privateDir);
        return privateDir;
    }

    @Override
    public WorkspaceMonitoringInformation getMonitoringInformation() throws IOException {
        return getMonitoringInformation(getTenantWorkspace());
    }

    private WorkspaceMonitoringInformation getMonitoringInformation(Path path) throws IOException {
        FileStore fileStore = Files.getFileStore(path);
        long totalSpace = fileStore.getTotalSpace();
        long usableSpace = fileStore.getUsableSpace();
        long usedSpace = totalSpace - usableSpace;
        return new WorkspaceMonitoringInformation(fileStore.name(),
                                                  totalSpace,
                                                  usedSpace,
                                                  usableSpace,
                                                  getMicroserviceWorkspace().toString());
    }

    @Override
    public Path getMicroserviceWorkspace() {
        return Paths.get(workspaceBasePath, runtimeTenantResolver.getTenant(), springApplicationName);
    }

    @Override
    public Path getTenantWorkspace() {
        return Paths.get(workspaceBasePath, runtimeTenantResolver.getTenant());
    }

    @Override
    public Path getFilePath(String fileName) {
        return Paths.get(getMicroserviceWorkspace().toString(), fileName);
    }

    @Scheduled(fixedDelay = 60 * 60000, initialDelay = 60000)
    public void monitorWorkspace() {
        for (String tenant : tenantResolver.getAllTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            publisher.publish(new WorkspaceMonitoringEvent());
            runtimeTenantResolver.clearTenant();
        }
    }

    private void monitor(String tenant) {
        try {
            WorkspaceMonitoringInformation workspaceMonitoringInfo = getMonitoringInformation(getTenantWorkspace());
            if (workspaceMonitoringInfo.getOccupationRatio() > workspaceOccupationThreshold) {
                String message = String.format("Workspace(%s) is too busy. Occupation is %s which is greater than %s",
                                               workspaceMonitoringInfo.getPath(),
                                               workspaceMonitoringInfo.getOccupationRatio().toString(),
                                               workspaceOccupationThreshold.toString());
                LOG.warn(message);
                MaintenanceManager.setMaintenance(tenant);
                notifier.sendErrorNotification(springApplicationName,
                                               message,
                                               "Workspace too busy",
                                               DefaultRole.PROJECT_ADMIN);
            }
        } catch (IOException e) {
            String message = String.format("Error occured during workspace monitoring: %s", e.getMessage());
            LOG.error(message, e);
            notifier.sendErrorNotification(springApplicationName,
                                           message,
                                           "Error during workspace monitoring",
                                           DefaultRole.PROJECT_ADMIN);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(WorkspaceMonitoringEvent.class, new WorkspaceMonitoringEventHandler());
    }

    private final class WorkspaceMonitoringEventHandler implements IHandler<WorkspaceMonitoringEvent> {

        @Override
        public void handle(TenantWrapper<WorkspaceMonitoringEvent> wrapper) {
            runtimeTenantResolver.forceTenant(wrapper.getTenant());
            monitor(wrapper.getTenant());
            runtimeTenantResolver.clearTenant();
        }
    }
}
