/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.service.entities.plugins;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.dao.entities.IAbstractEntityRequestRepository;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntityRequest;
import fr.cnes.regards.modules.dam.service.entities.IStorageService;
import fr.cnes.regards.modules.dam.service.settings.IDamSettingsService;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 */
@Plugin(description = "This plugin allows to POST AIP entities to storage unit",
        id = "StoragePlugin",
        version = "1.0.0",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class StoragePlugin implements IStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoragePlugin.class);

    private final static String URI_TEMPLATE = "%s?scope=%s";

    @Autowired
    private IDamSettingsService damSettingsService;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IAbstractEntityRequestRepository entityRequestRepo;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public <T extends AbstractEntity<?>> T store(T toPersist) {

        Collection<FileStorageRequestDTO> files = toPersist.getFiles()
                                                           .values()
                                                           .stream()
                                                           .map(entry -> initStorageRequest(entry, toPersist.getIpId()))
                                                           .collect(Collectors.toSet());

        if (!StringUtils.isEmpty(damSettingsService.getStorageLocation()) && (!files.isEmpty())) {
            Set<AbstractEntityRequest> infos = this.storageClient.store(files)
                                                                 .stream()
                                                                 .map(request -> new AbstractEntityRequest(request.getGroupId(),
                                                                                                           toPersist.getIpId()))
                                                                 .collect(Collectors.toSet());
            this.entityRequestRepo.saveAll(infos);
        } else {
            String message = "Data files are stored localy on datamanagement service as no storage location has been defined in microservice configuration.";
            String title = "Files stored locally";
            String[] users = new String[] { authResolver.getUser() };
            notificationClient.notify(message, title, NotificationLevel.INFO, users);
        }
        return toPersist;
    }

    @Override
    public <T extends AbstractEntity<?>> T update(T toUpdate, T oldEntity) {

        if (!StringUtils.isEmpty(damSettingsService.getStorageLocation())) {

            // manage added files in toUpdate and not in oldEntity
            Collection<FileStorageRequestDTO> filesToAdd = toUpdate.getFiles()
                                                                   .values()
                                                                   .stream()
                                                                   .filter(file -> !oldEntity.getFiles()
                                                                                             .values()
                                                                                             .stream()
                                                                                             .anyMatch(f -> f.getChecksum()
                                                                                                             .equals(
                                                                                                                 file.getChecksum())))
                                                                   .map(entry -> initStorageRequest(entry,
                                                                                                    toUpdate.getIpId()))
                                                                   .collect(Collectors.toSet());
            if (!filesToAdd.isEmpty()) {
                Set<AbstractEntityRequest> infos = this.storageClient.store(filesToAdd)
                                                                     .stream()
                                                                     .map(request -> new AbstractEntityRequest(request.getGroupId(),
                                                                                                               toUpdate.getIpId()))
                                                                     .collect(Collectors.toSet());
                this.entityRequestRepo.saveAll(infos);
            }
            // manage deleted file in toUpdate and present in oldEntity
            Collection<FileDeletionRequestDTO> filesToDelete = oldEntity.getFiles()
                                                                        .values()
                                                                        .stream()
                                                                        .filter(file -> !toUpdate.getFiles()
                                                                                                 .values()
                                                                                                 .stream()
                                                                                                 .anyMatch(f -> f.getChecksum()
                                                                                                                 .equals(
                                                                                                                     file.getChecksum())))
                                                                        .map(entry -> initDeletionRequest(entry,
                                                                                                          toUpdate.getIpId()
                                                                                                                  .toString()))
                                                                        .collect(Collectors.toSet());
            if (!filesToDelete.isEmpty()) {
                this.storageClient.delete(filesToDelete);
            }
        } else {
            LOGGER.info("[FILES UPDATE] Service not configured to store files with storage microservice.");
        }

        return toUpdate;
    }

    @Override
    public void delete(AbstractEntity<?> toDelete) {
        if (!StringUtils.isEmpty(damSettingsService.getStorageLocation())) {

            Collection<FileDeletionRequestDTO> files = toDelete.getFiles()
                                                               .values()
                                                               .stream()
                                                               .map(entry -> initDeletionRequest(entry,
                                                                                                 toDelete.getIpId()
                                                                                                         .toString()))
                                                               .collect(Collectors.toList());
            if (!files.isEmpty()) {
                this.storageClient.delete(files);
            }
        } else {
            LOGGER.info("[FILES DELETION] Service not configured to store files with storage microservice.");
        }
    }

    private FileStorageRequestDTO initStorageRequest(DataFile file, UniformResourceName urn) {
        return FileStorageRequestDTO.build(file.getFilename(),
                                           file.getChecksum(),
                                           file.getDigestAlgorithm(),
                                           file.getMimeType().toString(),
                                           urn.toString(),
                                           null,
                                           null,
                                           String.format(URI_TEMPLATE, file.getUri(), this.tenantResolver.getTenant()),
                                           damSettingsService.getStorageLocation(),
                                           Optional.ofNullable(damSettingsService.getStorageSubDirectory()));
    }

    private FileDeletionRequestDTO initDeletionRequest(DataFile file, String urn) {
        return FileDeletionRequestDTO.build(file.getChecksum(),
                                            damSettingsService.getStorageLocation(),
                                            urn,
                                            null,
                                            null,
                                            false);
    }
}
