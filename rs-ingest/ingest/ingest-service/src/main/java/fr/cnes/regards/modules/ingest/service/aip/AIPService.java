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

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.dam.dto.FeatureEvent;
import fr.cnes.regards.modules.ingest.dao.AIPEntitySpecification;
import fr.cnes.regards.modules.ingest.dao.AIPQueryGenerator;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdatesCreatorRepository;
import fr.cnes.regards.modules.ingest.dao.ICustomAIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.job.AIPUpdatesCreatorJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;

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

    public static final String MD5_ALGORITHM = "MD5";

    private static final String JSON_INDENT = "  ";

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IAIPUpdatesCreatorRepository aipUpdatesCreatorRepository;

    @Autowired
    private ICustomAIPRepository customAIPRepository;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IPublisher publisher;

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

    @Override
    public String calculateChecksum(AIP aip) throws NoSuchAlgorithmException, IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        writeAip(aip, os);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(os.toByteArray());
        return ChecksumUtils.computeHexChecksum(inputStream, MD5_ALGORITHM);
    }

    @Override
    public Page<AIPEntity> search(SearchAIPsParameters filters, Pageable pageable) {

        return aipRepository.findAll(AIPEntitySpecification.searchAll(filters, pageable), pageable);
    }

    @Override
    public List<String> searchTags(SearchFacetsAIPsParameters filters) {
        return customAIPRepository.getDistinct(AIPQueryGenerator.searchAipTagsUsingSQL(filters));
    }

    @Override
    public List<String> searchStorages(SearchFacetsAIPsParameters filters) {
        return customAIPRepository.getDistinct(AIPQueryGenerator.searchAipStoragesUsingSQL(filters));
    }

    @Override
    public List<String> searchCategories(SearchFacetsAIPsParameters filters) {
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
     * Write AIP in specified {@link OutputStream}
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

        // Mark these entities as deleted
        sessionNotifier.notifyAIPDeleting(aipsRelatedToSip);

        for (AIPEntity aipEntity : aipsRelatedToSip) {
            // Retrieve all files linked to this AIP
            for (ContentInformation ci : aipEntity.getAip().getProperties().getContentInformations()) {
                OAISDataObject dataObject = ci.getDataObject();
                for (OAISDataObjectLocation location : dataObject.getLocations()) {
                    if (location.getStorage() != null) {
                        // Create the storage delete event
                        filesToDelete.add(FileDeletionRequestDTO.build(dataObject.getChecksum(), location.getStorage(),
                                                                       aipEntity.getAipId(), false));
                    }
                }
            }

            // Add the AIP itself (on each storage) to the file list to remove
            for (StorageMetadata storage : aipEntity.getIngestMetadata().getStorages()) {
                filesToDelete.add(FileDeletionRequestDTO.build(aipEntity.getChecksum(), storage.getPluginBusinessId(),
                                                               aipEntity.getAipId(), false));
            }

            // Mark the AIP as deleted
            aipEntity.setState(AIPState.DELETED);
            aipRepository.save(aipEntity);
        }

        // Publish event to delete AIP files and AIPs itself
        RequestInfo deleteRequestInfo = storageClient.delete(filesToDelete);
        return deleteRequestInfo.getGroupId();
    }

    @Override
    public void scheduleAIPEntityUpdate(AIPUpdateParametersDto params) {
        AIPUpdatesCreatorRequest request = AIPUpdatesCreatorRequest.build(params);
        request = aipUpdatesCreatorRepository.save(request);
        // Schedule deletion job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(AIPUpdatesCreatorJob.REQUEST_ID, request.getId()));
        JobInfo jobInfo = new JobInfo(false, IngestJobPriority.UPDATE_AIP_SCAN_JOB_PRIORITY.getPriority(),
                jobParameters, authResolver.getUser(), AIPUpdatesCreatorJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
    }

    @Override
    public void processDeletion(String sipId, boolean deleteIrrevocably) {
        // Retrieve all AIP relative to this SIP id
        Set<AIPEntity> aipsRelatedToSip = aipRepository.findBySipSipId(sipId);
        sessionNotifier.notifyAIPDeleted(aipsRelatedToSip);
        if (!deleteIrrevocably) {
            for (AIPEntity aipEntity : aipsRelatedToSip) {
                aipEntity.setErrors(null);
                aipEntity.setState(AIPState.DELETED);
                save(aipEntity);
            }
        } else {
            // Delete them
            aipRepository.deleteAll(aipsRelatedToSip);
        }
        // Send notification to data mangement for feature deleted
        aipsRelatedToSip.forEach(aip -> publisher.publish(FeatureEvent.buildFeatureDeleted(aip.getAipId())));

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
    public void saveError(AIPEntity aipEntity, String errorMessage) {
        aipEntity.getErrors().add(errorMessage);
        aipEntity.setState(AIPState.ERROR);
        save(aipEntity);
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

    @Override
    public Collection<AIPEntity> getAips(Collection<String> aipIds) {
        return aipRepository.findByAipIdIn(aipIds);
    }

}
