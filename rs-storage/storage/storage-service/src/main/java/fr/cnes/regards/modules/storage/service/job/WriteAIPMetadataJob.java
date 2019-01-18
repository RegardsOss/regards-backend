/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.job;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationLevel;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.FileCorruptedException;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.service.IAIPService;

/**
 * Job to handle AIP metadata files write in temporary directory.
 * This job also schedule a job to store the metadata files written.
 *
 * @author Sébastien Binda
 */
public class WriteAIPMetadataJob extends AbstractJob<Void> {

    /**
     * JSON files extension.
     */
    public static final String JSON_FILE_EXT = ".json";

    /**
     * JOB parameter name containing all aip ids to handle metadata
     */
    public static final String AIP_IDS_TO_WRITE_METADATA = "aipIds";

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private Gson gson;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private INotificationClient notificationClient;

    private Set<String> aipIds;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        aipIds = parameters.get(AIP_IDS_TO_WRITE_METADATA).getValue();
    }

    @Override
    public void run() {
        if (aipIds != null && !aipIds.isEmpty()) {
            Set<StorageDataFile> metadataToStore = Sets.newHashSet();

            // Write metadata files into workspace
            for (String aipId : aipIds) {
                AIP aip;
                try {
                    aip = aipService.retrieveAip(aipId);
                    try {
                        logger.debug("[METADATA STORE] Writting meta-data for aip fully stored {}", aipId);
                        metadataToStore.add(writeMetaToWorkspace(aip));
                    } catch (IOException | FileCorruptedException e) {
                        logger.error(e.getMessage(), e);
                        aip.setState(AIPState.STORAGE_ERROR);
                        aipService.save(aip, true);
                    }
                } catch (EntityNotFoundException e1) {
                    String message = String
                            .format("Unable to write metadata file for AIP fully stored %s. The AIP does not exists anymore.",
                                    aipId);
                    FeignSecurityManager.asSystem();
                    try {
                        notificationClient.notify(message, "Storage error",

                                                  NotificationLevel.ERROR, DefaultRole.ADMIN);
                    } finally {
                        FeignSecurityManager.reset();
                    }
                }
            }

            // Now, schdule storage metadata job for each file
            aipService.scheduleStorageMetadata(metadataToStore);
        }
    }

    /**
     * Write on disk the associated metadata file of the given {@link AIP}.
     * @param aip {@link AIP}
     * @return {@link StorageDataFile} of the {@link AIP} metadata file.
     * @throws IOException Impossible to write {@link AIP} metadata file to disk.
     */
    private StorageDataFile writeMetaToWorkspace(AIP aip) throws IOException, FileCorruptedException {

        StorageDataFile metadataAipFile;
        String checksumAlgorithm = "MD5";
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        String toWrite = gson.toJson(aip);
        String checksum = ChecksumUtils.getHexChecksum(md5.digest(toWrite.getBytes(StandardCharsets.UTF_8)));
        String metadataName = checksum + JSON_FILE_EXT;
        workspaceService.setIntoWorkspace(new ByteArrayInputStream(toWrite.getBytes(StandardCharsets.UTF_8)),
                                          metadataName);
        try (InputStream is = workspaceService.retrieveFromWorkspace(metadataName)) {
            String fileChecksum = ChecksumUtils.computeHexChecksum(is, checksumAlgorithm);
            if (fileChecksum.equals(checksum)) {
                URL urlToMetadata = new URL("file", "localhost",
                        workspaceService.getFilePath(metadataName).toAbsolutePath().toString());
                AIPSession aipSession = aipService.getSession(aip.getSession(), false);
                metadataAipFile = new StorageDataFile(Sets.newHashSet(urlToMetadata), checksum, checksumAlgorithm,
                        DataType.AIP, urlToMetadata.openConnection().getContentLengthLong(),
                        new MimeType("application", "json"), new AIPEntity(aip, aipSession),
                        aip.getId().toString() + JSON_FILE_EXT, null);
                metadataAipFile.setState(DataFileState.PENDING);
            } else {
                workspaceService.removeFromWorkspace(metadataName);
                logger.error(String
                        .format("Storage of AIP metadata(%s) into workspace(%s) failed. Computed checksum once stored does not "
                                + "match expected one", aip.getId().toString(),
                                workspaceService.getMicroserviceWorkspace()));
                throw new FileCorruptedException(String
                        .format("File has been corrupted during storage into workspace. Checksums before(%s) and after (%s) are"
                                + " different", checksum, fileChecksum));
            }
        } catch (NoSuchAlgorithmException e) {
            // Delete written file
            logger.error(e.getMessage(), e);
            workspaceService.removeFromWorkspace(metadataName);
            // this exception should never be thrown as it comes from the same algorithm then at the beginning
            throw new IOException(e);
        } catch (EntityNotFoundException e) {
            // Delete written file
            logger.error(e.getMessage(), e);
            workspaceService.removeFromWorkspace(metadataName);
            // this exception should never be thrown because is already created and so is the session
            throw new RsRuntimeException(e);
        }
        return metadataAipFile;
    }

}
