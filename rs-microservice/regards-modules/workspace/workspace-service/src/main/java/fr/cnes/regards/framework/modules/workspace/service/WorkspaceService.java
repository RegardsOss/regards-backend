/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * Default {@link IWorkspaceService} implementation which divide the workspace according {@link #getMicroserviceWorkspace()} implementation.
 * @author svissier
 */
@Service
@ConditionalOnMissingBean(value = IWorkspaceService.class)
public class WorkspaceService implements IWorkspaceService, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Workspace service logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    private static final String CRITICAL_MESSAGE_FORMAT = "Workspace \"%s\" occupation is critical. Occupation is \"%.2f%%\" which is greater than \"%d%%\" (critical threshold). Project \"%s\" is being set to maintenance mode!";

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
    @Value("${regards.workspace.occupation.threshold:70}")
    private Integer workspaceOccupationThreshold;

    /**
     * The workspace critical occupation threshold at which point notification should be sent and projet set to
     * maintenance
     */
    @Value("${regards.workspace.critical.occupation.threshold:90}")
    private Integer workspaceCriticalOccupationThreshold;

    /**
     * The name of the subdirectory where to store microservice workspace.
     */
    @Value("${microservice.workspace.directory.name:${spring.application.name}}")
    private String microserviceWorkspaceName;

    @Override
    public void setIntoWorkspace(InputStream is, String fileName) throws IOException {
        // first lets check if the wroskapce occupation is not critical
        WorkspaceMonitoringInformation workspaceMonitoringInfo = getMonitoringInformation(getTenantWorkspace());
        if (workspaceMonitoringInfo.getOccupationRatio() > workspaceCriticalOccupationThreshold) {
            String message = String.format(CRITICAL_MESSAGE_FORMAT, workspaceMonitoringInfo.getPath(),
                                           workspaceMonitoringInfo.getOccupationRatio() * 100,
                                           workspaceCriticalOccupationThreshold, runtimeTenantResolver.getTenant());
            LOG.warn(message);
            MaintenanceManager.setMaintenance(runtimeTenantResolver.getTenant());
            notifier.sendErrorNotification(message, "Workspace occupation is critical", DefaultRole.PROJECT_ADMIN);
        }
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
        return new WorkspaceMonitoringInformation(fileStore.name(), totalSpace, usedSpace, usableSpace,
                getMicroserviceWorkspace().toString());
    }

    @Override
    public Path getMicroserviceWorkspace() throws IOException {
        Path path = Paths.get(workspaceBasePath, runtimeTenantResolver.getTenant(), microserviceWorkspaceName);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    @Override
    public Path getTenantWorkspace() throws IOException {
        Path path = Paths.get(workspaceBasePath, runtimeTenantResolver.getTenant());
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    @Override
    public Path getFilePath(String fileName) throws IOException {
        return Paths.get(getMicroserviceWorkspace().toString(), fileName);
    }

    @Scheduled(fixedDelayString = "${regards.workspace.monitoring.delay.ms:3600000}", initialDelay = 60000)
    public void monitorWorkspace() {
        for (String tenant : tenantResolver.getAllTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            publisher.publish(new WorkspaceMonitoringEvent());
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public void monitor(String tenant) {
        try {
            WorkspaceMonitoringInformation workspaceMonitoringInfo = getMonitoringInformation(getTenantWorkspace());
            if (workspaceMonitoringInfo.getOccupationRatio() * 100 > workspaceCriticalOccupationThreshold) {
                String message = String.format(CRITICAL_MESSAGE_FORMAT, workspaceMonitoringInfo.getPath(),
                                               workspaceMonitoringInfo.getOccupationRatio() * 100,
                                               workspaceCriticalOccupationThreshold, tenant);
                LOG.warn(message);
                MaintenanceManager.setMaintenance(tenant);
                notifier.sendErrorNotification(message, "Workspace occupation is critical", DefaultRole.PROJECT_ADMIN);
                return;
            }
            if (workspaceMonitoringInfo.getOccupationRatio() * 100 > workspaceOccupationThreshold) {
                String message = String
                        .format("Workspace \"%s\" for project \"%s\" starts to be busy. Occupation is \"%.2f%%\" which is greater than \"%d%%\" (soft threshold).",
                                workspaceMonitoringInfo.getPath(), tenant,
                                workspaceMonitoringInfo.getOccupationRatio() * 100, workspaceOccupationThreshold);
                LOG.warn(message);
                notifier.sendWarningNotification(message, "Workspace too busy", DefaultRole.PROJECT_ADMIN);
                return;
            }
        } catch (IOException e) {
            String message = String.format("Error occured during workspace monitoring: %s", e.getMessage());
            LOG.error(message, e);
            notifier.sendErrorNotification(message, "Error during workspace monitoring", DefaultRole.PROJECT_ADMIN);
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
