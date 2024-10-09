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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.dao.PackageReferenceRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.file.packager.domain.PackageReference;
import fr.cnes.regards.modules.file.packager.domain.PackageReferenceStatus;
import fr.cnes.regards.modules.filecatalog.amqp.input.FileArchiveResponseEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileArchiveRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for file packaging.
 * The workflow is as follows :
 * <ul>
 *     <li>Files to package are received through the
 *     {@link fr.cnes.regards.modules.file.packager.service.handler.FileArchiveRequestEventHandler FileArchiveRequestEventHandler}</li>
 *     <li>For each file, an entity {@link FileInBuildingPackage} is saved in the method
 *     {@link #createNewFilesInBuildingPackage(List) createNewFilesInBuildingPackage}</li>
 *     <li>The scheduler {@link fr.cnes.regards.modules.file.packager.service.scheduler.FilePackagingScheduler
 *     FilePackagingScheduler} will associate the {@link FileInBuildingPackage} with {@link PackageReference} using
 *     the method {@link #associateFilesToPackage(Pageable) associateFilesToPackage}
 *     .</li>
 *     <li>After a file association, the scheduler will verify that there is still room in the package to add new
 *     files, otherwise it will close it and mark it to be send</li>
 *     <li>The scheduler {@link fr.cnes.regards.modules.file.packager.service.scheduler.FilePackagingScheduler
 *      FilePackagingScheduler} will close package that are too old even if they're not full </li> using the method
 *      {@link #closeOldPackages()}
 *     <li>WIP To be continued ...</li>
 * </ul>
 *
 * @author Thibaud Michaudel
 **/
@Service
public class FilePackagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePackagerService.class);

    private final FileInBuildingPackageRepository fileInBuildingPackageRepository;

    private final PackageReferenceRepository packageReferenceRepository;

    private final IPublisher publisher;

    @Value("${regards.file.packager.archive.max.size.in.ko:1024}")
    private int maxArchiveSizeInKo;

    @Value("${regards.file.packager.archive.max.age.in.hours:24}")
    private int maxArchiveAgeInHours;

    private final DateTimeFormatter archiveNameFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final String FILE_IN_ARCHIVE = ".zip?fileName=";

    public FilePackagerService(FileInBuildingPackageRepository fileInBuildingPackageRepository,
                               PackageReferenceRepository packageReferenceRepository,
                               IPublisher publisher) {
        this.fileInBuildingPackageRepository = fileInBuildingPackageRepository;
        this.packageReferenceRepository = packageReferenceRepository;
        this.publisher = publisher;
    }

    /**
     *
     */
    @MultitenantTransactional
    public void createNewFilesInBuildingPackage(List<FileArchiveRequestEvent> messages) {
        List<FileInBuildingPackage> files = messages.stream()
                                                    .map(message -> new FileInBuildingPackage(message.getFileStorageRequestId(),
                                                                                              message.getStorage(),
                                                                                              message.getChecksum(),
                                                                                              message.getFileName(),
                                                                                              message.getStorageSubDirectory(),
                                                                                              message.getFinalArchiveParentUrl(),
                                                                                              message.getFileCachePath(),
                                                                                              message.getFileSize()))
                                                    .toList();
        fileInBuildingPackageRepository.saveAll(files);
    }

    /**
     * Associate the {@link FileInBuildingPackage}s in a page to different {@link PackageReference} based on the
     * storage and parentPath of the FileInBuildingPackage.
     */
    @MultitenantTransactional
    public void associateFilesToPackage(Pageable page) {
        // Get FilesToPackage
        Page<FileInBuildingPackage> filesPage = findFilesToPackage(page);

        // Regroup by storage and store parent url (the url contains the file's node)
        Map<StorageAndPath, List<FileInBuildingPackage>> filePackageMap = new HashMap<>();
        filesPage.getContent()
                 .forEach(file -> filePackageMap.computeIfAbsent(new StorageAndPath(file.getStorage(),
                                                                                    file.getStorageSubdirectory()),
                                                                 k -> new ArrayList<>()).add(file));

        // Associate the files
        filePackageMap.forEach((key, value) -> associateFilesToPackage(key.storage(), key.path(), value));
    }

    /**
     * Associate the list of {@link FileInBuildingPackage}s to one or more {@link PackageReference}s with the given
     * storage and path.
     */
    private void associateFilesToPackage(String storage, String path, List<FileInBuildingPackage> files) {

        // Package that may be existing (from a previous build)
        Optional<PackageReference> existingPackageReference = packageReferenceRepository.findOneByStorageAndStorageSubdirectoryAndStatus(
            storage,
            path,
            PackageReferenceStatus.BUILDING);

        // List of events to send (one for each associated file)
        List<FileArchiveResponseEvent> responsesToSend = new ArrayList<>();

        for (FileInBuildingPackage file : files) {
            // Create new package if there is no incomplete one
            // Save it as soon as its created, so it can be referenced by the file

            PackageReference packageReference;
            packageReference = existingPackageReference.orElseGet(() -> packageReferenceRepository.save(new PackageReference(
                storage,
                path)));

            // Compute the file url on the storage
            try {
                String finalFileUrl = createFinalFileUrl(file.getFinalArchiveParentUrl(),
                                                         packageReference.getCreationDate(),
                                                         file.getFilename());
                // Associate the file with the package
                file.setPackageReference(packageReference);
                file.updateStatus(FileInBuildingPackageStatus.BUILDING, null);

                // Save response with the final file url
                FileArchiveResponseEvent responseEvent = new FileArchiveResponseEvent(file.getStorageRequestId(),
                                                                                      file.getStorage(),
                                                                                      file.getChecksum(),
                                                                                      finalFileUrl);
                responsesToSend.add(responseEvent);

                // Update package size
                packageReference.addFileSize(file.getFileSize());

                // Close package if it's full, otherwise continue to fill it
                if (packageReference.getSize() > maxArchiveSizeInKo * 1024L) {
                    packageReference.setStatus(PackageReferenceStatus.TO_STORE);
                    packageReferenceRepository.save(packageReference);
                    existingPackageReference = Optional.empty();
                } else {
                    existingPackageReference = Optional.of(packageReference);
                }
            } catch (URISyntaxException e) {
                LOGGER.error("Error while computing final file url.", e);
                file.updateStatus(FileInBuildingPackageStatus.BUILDING_ERROR, "Error while computing final file url");
            }
        }

        // Send the events
        publisher.publish(responsesToSend);

        // Save all the files
        fileInBuildingPackageRepository.saveAll(files);
    }

    /**
     * Create the url of a file that will be stored in the file-catalog database.
     * This url will not really link to the file, but the file-packager will be able to interpret it to link to the
     * actual file location.
     */
    private String createFinalFileUrl(String finalArchiveParentUrl, OffsetDateTime packageCreationDate, String fileName)
        throws URISyntaxException {

        String archiveName = packageCreationDate.format(archiveNameFormatter);
        return finalArchiveParentUrl + archiveName + FILE_IN_ARCHIVE + fileName;
    }

    private Page<FileInBuildingPackage> findFilesToPackage(Pageable page) {
        return fileInBuildingPackageRepository.findByStatusOrderByStorageAscStorageSubdirectoryAsc(
            FileInBuildingPackageStatus.WAITING_PACKAGE,
            page);
    }

    /**
     * Set status {@link PackageReferenceStatus#TO_STORE TO_STORE} to all packages in status
     * {@link PackageReferenceStatus#BUILDING BUILDING} that are older than the allowed age.
     * The relevant packages are the one where {@link PackageReference#getCreationDate()} < now - maxArchiveAge.
     */
    @MultitenantTransactional
    public void closeOldPackages() {
        packageReferenceRepository.closeAllOldPackages(OffsetDateTime.now().minusHours(maxArchiveAgeInHours));
    }

    private record StorageAndPath(String storage,
                                  String path) {

    }
}
