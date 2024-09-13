/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.file.packager.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.file.packager.service.handler.FileArchiveRequestEventHandler;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileArchiveRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for file packaging.
 * The workflow is as follows :
 * <ul>
 *     <li>Files to package are received through the {@link FileArchiveRequestEventHandler}</li>
 *     <li>For each file, an entity {@link FileInBuildingPackage} is saved in status
 *     {@link FileInBuildingPackageStatus#WAITING_PACKAGE} in the method {@link #createNewFilesInBuildingPackage(List) createNewFilesInBuildingPackage}</li>
 *     <li>WIP to be continued...</li>
 * </ul>
 *
 * @author Thibaud Michaudel
 **/
@Service
@MultitenantTransactional
public class FilePackagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePackagerService.class);

    private final FileInBuildingPackageRepository fileInBuildingPackageRepository;

    public FilePackagerService(FileInBuildingPackageRepository fileInBuildingPackageRepository) {
        this.fileInBuildingPackageRepository = fileInBuildingPackageRepository;
    }

    @MultitenantTransactional
    public void createNewFilesInBuildingPackage(List<FileArchiveRequestEvent> messages) {
        List<FileInBuildingPackage> files = messages.stream()
                                                    .map(message -> new FileInBuildingPackage(message.getFileStorageRequestId(),
                                                                                              message.getStorage(),
                                                                                              message.getChecksum(),
                                                                                              message.getFileName(),
                                                                                              message.getCurrentFileParentPath(),
                                                                                              message.getFinalArchiveParentUrl(),
                                                                                              message.getFileSize()))
                                                    .toList();
        fileInBuildingPackageRepository.saveAll(files);
    }
}
