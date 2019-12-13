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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.dam.dto.FeatureEvent;
import fr.cnes.regards.modules.ingest.dao.AIPEntitySpecification;
import fr.cnes.regards.modules.ingest.dao.AIPQueryGenerator;
import fr.cnes.regards.modules.ingest.dao.IAIPLightRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ICustomAIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.AbstractSearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.request.IOAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
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

/**
 * AIP service management
 *
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class AIPService implements IAIPService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPService.class);

    public static final String MD5_ALGORITHM = "MD5";

    private static final String JSON_INDENT = "  ";

    @Autowired
    private IOAISDeletionService oaisDeletionRequestService;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IAIPLightRepository aipLigthRepository;

    @Autowired
    private ICustomAIPRepository customAIPRepository;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Gson gson;

    @Autowired
    private IRequestService requestService;

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

    @Override
    public String calculateChecksum(AIP aip) throws NoSuchAlgorithmException, IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        writeAip(aip, os);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(os.toByteArray());
        return ChecksumUtils.computeHexChecksum(inputStream, MD5_ALGORITHM);
    }

    @Override
    public Page<AIPEntity> findByFilters(AbstractSearchAIPsParameters<?> filters, Pageable pageable) {
        return aipRepository.findAll(AIPEntitySpecification.searchAll(filters, pageable), pageable);
    }

    @Override
    public Collection<AIPEntity> findByAipIds(Collection<String> aipIds) {
        return aipRepository.findByAipIdIn(aipIds);
    }


    @Override
    public Page<AIPEntityLight> findLightByFilters(AbstractSearchAIPsParameters<?> filters, Pageable pageable) {
        LOGGER.debug("Searching AIPS with categories=[{}]...", String.join(",", filters.getCategories()));
        long start = System.currentTimeMillis();
        Page<AIPEntityLight> response = aipLigthRepository.findAll(AIPEntitySpecification.searchAll(filters, pageable),
                                                                   pageable);
        LOGGER.debug("{} AIPS found in  {}ms", response.getSize(), System.currentTimeMillis() - start);
        return response;
    }

    @Override
    public List<String> findTags(SearchFacetsAIPsParameters filters) {
        return customAIPRepository.getDistinct(AIPQueryGenerator.searchAipTagsUsingSQL(filters));
    }

    @Override
    public List<String> findStorages(SearchFacetsAIPsParameters filters) {
        return customAIPRepository.getDistinct(AIPQueryGenerator.searchAipStoragesUsingSQL(filters));
    }

    @Override
    public List<String> findCategories(SearchFacetsAIPsParameters filters) {
        return customAIPRepository.getDistinct(AIPQueryGenerator.searchAipCategoriesUsingSQL(filters));
    }

    @Override
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
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + aip.getProviderId() + ".json");
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
     * Write AIP in provided {@link OutputStream}
     */
    private void writeAip(AIP aip, OutputStream os) throws IOException {
        Writer osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        JsonWriter writer = new JsonWriter(osw);
        if (aip.getNormalizedGeometry() == null) {
            aip.setNormalizedGeometry(IGeometry.unlocated());
        }
        writer.setIndent(JSON_INDENT);
        gson.toJson(aip, AIP.class, writer);
        osw.flush();
        writer.flush();
    }

    @Override
    public void scheduleLinkedFilesDeletion(OAISDeletionRequest request) {
        String sipId = request.getAip().getSip().getSipId();
        List<FileDeletionRequestDTO> filesToDelete = new ArrayList<>();

        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aips = aipRepository.findBySipSipId(sipId);

        for (AIPEntity aipEntity : aips) {
            String aipId = aipEntity.getAipId();
            // Retrieve all linked files
            for (ContentInformation ci : aipEntity.getAip().getProperties().getContentInformations()) {
                OAISDataObject dataObject = ci.getDataObject();
                filesToDelete.addAll(getFileDeletionEvents(aipId, dataObject.getChecksum(),
                        dataObject.getLocations()));
            }

            // Add the AIP itself (on each storage) to the file list to remove
            filesToDelete.addAll(getFileDeletionEvents(aipId, aipEntity.getChecksum(), aipEntity.getManifestLocations()));
        }

        // Publish event to delete AIP files and AIPs itself
        RequestInfo deleteRequestInfo = storageClient.delete(filesToDelete);

        String deleteRequestId = deleteRequestInfo.getGroupId();
        request.setRemoteStepGroupIds(Lists.newArrayList(deleteRequestId));
        request.setWaitStorageAnswer();
        // Put the request as un-schedule.
        // The answering event from storage will put again the request to be executed
        request.setState(InternalRequestState.TO_SCHEDULE);
        oaisDeletionRequestService.update(request);
    }

    private List<FileDeletionRequestDTO> getFileDeletionEvents(String owner, String fileChecksum, Set<OAISDataObjectLocation> locations) {
        List<FileDeletionRequestDTO> events = new ArrayList<>();
        for (OAISDataObjectLocation location : locations) {
            // Ignore if the file is yet stored
            if (location.getStorage() != null) {
                // Create the storage delete event
                events.add(FileDeletionRequestDTO.build(fileChecksum, location.getStorage(),
                        owner, false));
            }
        }
        return events;
    }

    @Override
    public void registerUpdatesCreator(AIPUpdateParametersDto params) {
        AIPUpdatesCreatorRequest request = AIPUpdatesCreatorRequest.build(params);
        request = (AIPUpdatesCreatorRequest) requestService.scheduleRequest(request);
        if (request.getState() != InternalRequestState.BLOCKED) {
            requestService.scheduleJob(request);
        }
    }

    @Override
    public void processDeletion(String sipId, boolean deleteIrrevocably) {
        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aipsRelatedToSip = aipRepository.findBySipSipId(sipId);
        if (!aipsRelatedToSip.isEmpty()) {
            aipsRelatedToSip.forEach(entity -> {
                sessionNotifier.productDeleted(entity.getSessionOwner(), entity.getSession(), aipsRelatedToSip);
                entity.setState(AIPState.DELETED);
            });
            if (deleteIrrevocably) {
                requestService.deleteAllByAip(aipsRelatedToSip);
                // Delete them
                aipRepository.deleteAll(aipsRelatedToSip);
            } else {
                // Mark the AIP as deleted
                aipRepository.saveAll(aipsRelatedToSip);
            }
            // Send notification to data mangement for feature deleted
            aipsRelatedToSip.forEach(aip -> publisher.publish(FeatureEvent.buildFeatureDeleted(aip.getAipId())));
        }
    }

    @Override
    public Optional<AIPEntity> getAip(UniformResourceName aipId) {
        return aipRepository.findByAipId(aipId.toString());
    }

    @Override
    public Set<AIPEntity> findByAipIds(String sipId) {
        return aipRepository.findBySipSipId(sipId);
    }

    @Override
    public List<AIPEntity> saveAll(Collection<AIPEntity> entities) {
        for (AIPEntity aip : entities) {
            aip.setLastUpdate(OffsetDateTime.now());
        }
        return aipRepository.saveAll(entities);
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
