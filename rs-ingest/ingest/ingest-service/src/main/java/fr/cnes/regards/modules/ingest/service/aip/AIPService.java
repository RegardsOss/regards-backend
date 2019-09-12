/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.dao.AIPEntitySpecification;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileStorageRequestDTO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationProperties;

/**
 * AIP service management
 *
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
@Service
@MultitenantTransactional
public class AIPService implements IAIPService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPService.class);

    private static final String UTF8_ENCODING = "UTF-8";

    private static final String MD5_ALGORITHM = "MD5";

    private static final String JSON_INDENT = "  ";

    @Autowired
    private IngestConfigurationProperties confProperties;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private Gson gson;

    @Override
    public List<AIPEntity> createAndSave(SIPEntity sip, List<AIP> aips) {
        List<AIPEntity> entities = new ArrayList<>();
        for (AIP aip : aips) {
            entities.add(aipRepository.save(AIPEntity.build(sip, AIPState.GENERATED, aip)));
        }
        return entities;
    }

    @Override
    public AIPEntity save(AIPEntity entity) {
        entity.setLastUpdate(OffsetDateTime.now());
        return aipRepository.save(entity);
    }


    public Collection<FileStorageRequestDTO> buildAIPStorageRequest(AIP aip, String checksum, List<StorageMetadata> storages)
            throws ModuleException {

        // Build file storage requests
        Collection<FileStorageRequestDTO> files = new ArrayList<>();

        try {

            // Build origin(s) URL
            URL originUrl = new URI(confProperties.getAipDownloadTemplate()
                    .replace(IngestConfigurationProperties.DOWNLOAD_AIP_PLACEHOLDER, aip.getId().toString())).toURL();

            // Create a request for each storage
            for (StorageMetadata storage : storages) {
                files.add(FileStorageRequestDTO.build(aip.getId().toString(), checksum, MD5_ALGORITHM,
                                                      MediaType.APPLICATION_JSON_UTF8_VALUE, aip.getId().toString(),
                                                      originUrl.toString(), storage.getStorage(),
                                                      Optional.ofNullable(storage.getStorePath())));
            }
        } catch (URISyntaxException | IOException e) {
            String message = String.format("Error with building storage request for AIP %s", aip.getId());
            LOGGER.error(message, e);
            throw new ModuleException(message, e);
        }

        return files;
    }

    private String calculateChecksum(AIP aip) throws NoSuchAlgorithmException, IOException {
        // TODO @msordi si t'as des idées
//        try (PipedInputStream in = new PipedInputStream(); PipedOutputStream out = new PipedOutputStream(in)) {
//            writeAip(aip, out);
//            return ChecksumUtils.computeHexChecksum(in, MD5_ALGORITHM);
//        }
        String jsonSip = gson.toJson(aip);
        InputStream inputStream = new ByteArrayInputStream(jsonSip.getBytes());
        return ChecksumUtils.computeHexChecksum(inputStream, MD5_ALGORITHM);
    }

    @Override
    public Page<AIPEntity> search(AIPState state, OffsetDateTime from, OffsetDateTime to, List<String> tags, String sessionOwner,
            String session, String providerId, List<String> storages, List<String> categories, Pageable pageable) {

        return aipRepository.findAll(AIPEntitySpecification.searchAll(state, from, to, tags, sessionOwner, session,
                providerId, storages, categories, pageable), pageable);
    }

    public void downloadAIP(UniformResourceName aipId, HttpServletResponse response) throws ModuleException {

        // Find AIP
        AIPEntity aipEntity = aipRepository.findByAipId(aipId.toString()).orElse(null);
        if (aipEntity == null) {
            String message = String.format("AIP with URN %s not found!", aipId);
            LOGGER.error(message);
            throw new EntityNotFoundException(message);
        }

        AIP aip = aipEntity.getAip();

        // Populate response
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + aip.getId().toString());
        // NOTE : Do not set content type after download. It can be ignored.
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        // Stream AIP file
        try {
            writeAip(aip, response.getOutputStream());
        } catch (IOException e) {
            String message = String.format("Error occurs while trying to stream AIP file with URN %s!", aip.getId());
            LOGGER.error(message, e);
            throw new EntityException(message, e);
        }
    }

    /**
     * Write AIP in specified {@link OutputStream}
     */
    private void writeAip(AIP aip, OutputStream out) throws IOException {
//        try () {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, UTF8_ENCODING));
            writer.setIndent(JSON_INDENT);
            gson.toJson(aip, AIP.class, writer);
//        }
    }

    @Override
    public void setAipToStored(UniformResourceName aipId, AIPState state) {
        // Retrieve aip and set the new status to stored
        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        if (oAip.isPresent()) {
            AIPEntity aip = oAip.get();
            aip.setState(state);
            aip.setErrors(null);
            aipRepository.save(aip);
        }
    }


    @Override
    public String scheduleAIPEntityDeletion(String sipId) {
        List<FileDeletionRequestDTO> filesToDelete = new ArrayList<>();

        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aipsRelatedToSip = aipRepository.findBySipSipId(sipId);

        for (AIPEntity aipEntity : aipsRelatedToSip) {
            // Retrieve all files linked to this AIP
            for (ContentInformation ci : aipEntity.getAip().getProperties().getContentInformations()) {
                OAISDataObject dataObject = ci.getDataObject();
                for (OAISDataObjectLocation location : dataObject.getLocations()) {
                    if (location.getStorage() != null) {
                        // Create the storage delete event
                        filesToDelete.add(FileDeletionRequestDTO.build(dataObject.getChecksum(),
                                location.getStorage(), aipEntity.getAipId(), false));
                    }
                }
            }

            // Add the AIP itself (on each storage) to the file list to remove
            for (StorageMetadata storage: aipEntity.getIngestMetadata().getStorages()) {
                filesToDelete.add(FileDeletionRequestDTO.build(aipEntity.getChecksum(),
                        storage.getStorage(), aipEntity.getAipId(), false));
            }

            // Mark the AIP as deleted
            aipEntity.setState(AIPState.DELETED);
            aipRepository.save(aipEntity);
        }
        // Mark this entity as deleting
        sessionNotifier.notifyAIPDeleting(aipsRelatedToSip);

        // Publish event to delete AIP files and AIPs itself
        RequestInfo deleteRequestInfo = storageClient.delete(filesToDelete);
        return deleteRequestInfo.getGroupId();
    }

    @Override
    public void deleteIrrevocably(String sipId) {
        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aipsRelatedToSip = aipRepository.findBySipSipId(sipId);

        sessionNotifier.notifyAIPDeleted(aipsRelatedToSip);

        // Delete them
        aipRepository.deleteAll(aipsRelatedToSip);
    }

    @Override
    public Optional<AIPEntity> getAip(UniformResourceName aipId) {
        return aipRepository.findByAipId(aipId.toString());
    }

    @Override
    public Set<AIPEntity> getAips(String sipId) {
        return aipRepository.findBySipSipId(sipId);
    }

    @Override
    public void computeAndSaveChecksum(AIPEntity aipEntity) throws ModuleException {
        try {
            String checksum = calculateChecksum(aipEntity.getAip());
            aipEntity.setChecksum(checksum);
            save(aipEntity);
        } catch (IOException | NoSuchAlgorithmException e) {
            String message = String.format("Failed to compute AIP checksum for AIP %s", aipEntity.getId());
            LOGGER.error(message, e);
            throw new ModuleException(message, e);
        }
    }


}
