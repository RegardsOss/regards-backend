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
import fr.cnes.regards.framework.modules.workspace.domain.WorkspaceMonitoringInformation;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
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

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IWorkspaceNotifier notifier;

    @Value("${regards.workspace}")
    private String workspacePath;

    @Value("${regards.workspace.occupation.threshold:90}")
    private Integer workspaceOccupationThreshold;

    @Value("${spring.application.name}")
    private String springApplicationName;

    private Path microserviceWorkspace;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        microserviceWorkspace = Paths.get(workspacePath, springApplicationName);
        if(Files.notExists(microserviceWorkspace)) {
            try {
                Files.createDirectories(microserviceWorkspace);
            } catch (IOException e) {
                throw new IllegalStateException("Could not initialize workspace:", e);
            }
        }
    }

    @Override
    public void setIntoWorkspace(InputStream is, String fileName) throws IOException {
        String tenant = runtimeTenantResolver.getTenant();
        if (Files.notExists(Paths.get(microserviceWorkspace.toString(), tenant))) {
            Files.createDirectories(Paths.get(microserviceWorkspace.toString(), tenant));
        }
        Path pathInWS = Paths.get(microserviceWorkspace.toString(), tenant, fileName);
        OutputStream os = Files.newOutputStream(pathInWS, StandardOpenOption.CREATE);
        ByteStreams.copy(is, os);
        os.flush();
        os.close();
    }

    @Override
    public InputStream retrieveFromWorkspace(String fileName) throws IOException {
        String tenant = runtimeTenantResolver.getTenant();
        return Files.newInputStream(Paths.get(microserviceWorkspace.toString(), tenant, fileName));
    }

    @Override
    public void removeFromWorkspace(String fileName) throws IOException {
        String tenant = runtimeTenantResolver.getTenant();
        Files.deleteIfExists(Paths.get(microserviceWorkspace.toString(), tenant, fileName));
    }

    @Override
    public Path getPrivateDirectory() throws IOException {
        String tenant = runtimeTenantResolver.getTenant();
        Path privateDir = Paths.get(microserviceWorkspace.toString(), tenant, UUID.randomUUID().toString());
        Files.createDirectories(privateDir);
        return privateDir;
    }

    @Override
    public WorkspaceMonitoringInformation getMonitoringInformation() throws IOException {
        return getMonitoringInformation(microserviceWorkspace);
    }

    @Override
    public Path getMicroserviceWorkspace() {
        return microserviceWorkspace;
    }

    @Scheduled(fixedDelay = 60 * 60000)
    public void monitorWorkspace() {
        try {
            WorkspaceMonitoringInformation workspaceMonitoringInfo = getMonitoringInformation(Paths.get(workspacePath));
            if (workspaceMonitoringInfo.getOccupationRatio() > workspaceOccupationThreshold) {
                String message = String.format("Workspace is too busy. Occupation is %s which is greater than %s",
                                               workspaceMonitoringInfo.getOccupationRatio().toString(),
                                               workspaceOccupationThreshold.toString());
                LOG.warn(message);
                //TODO: set maintenance
                notifier.sendErrorNotification(springApplicationName,
                                               message,
                                               "Workspace too busy",
                                               DefaultRole.INSTANCE_ADMIN);
            }
        } catch (IOException e) {
            String message = String.format("Error occured during workspace monitoring: %s", e.getMessage());
            LOG.error(message, e);
            notifier.sendErrorNotification(springApplicationName,
                                           message,
                                           "Error during workspace monitoring",
                                           DefaultRole.INSTANCE_ADMIN);
        }
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
                                                  microserviceWorkspace.toString());
    }
}
