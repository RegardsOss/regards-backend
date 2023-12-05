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
package fr.cnes.regards.modules.ingest.service.aip;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.dao.*;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.aip.LastAIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.request.IOAISDeletionService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;

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

    public static final String MD5_ALGORITHM = "MD5";

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPService.class);

    private static final String JSON_INDENT = "  ";

    @Autowired
    private IOAISDeletionService oaisDeletionRequestService;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ILastAIPRepository lastAipRepository;

    @Autowired
    private IAIPLightRepository aipLigthRepository;

    @Autowired
    private ICustomAIPRepository customAIPRepository;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private Gson gson;

    @Autowired
    private IRequestService requestService;

    @Override
    public List<AIPEntity> createAndSave(SIPEntity sip, List<AIPDto> aips) {
        List<AIPEntity> entities = new ArrayList<>();
        for (AIPDto aip : aips) {
            entities.add(aipRepository.save(AIPEntity.build(sip, AIPState.GENERATED, aip)));
        }
        return entities;
    }

    @Override
    public AIPEntity updateLastFlag(AIPEntity aip, boolean last) {
        aip.setLast(last);
        save(aip); // Set id if not already set
        if (aip.isLast()) {
            lastAipRepository.save(new LastAIPEntity(aip.getId(), aip.getProviderId()));
        } else {
            lastAipRepository.deleteByAipId(aip.getId());
        }
        return aip;
    }

    @Override
    public AIPEntity save(AIPEntity entity) {
        entity.setLastUpdate(OffsetDateTime.now());
        return aipRepository.save(entity);
    }

    @Override
    public String calculateChecksum(AIPDto aip) throws NoSuchAlgorithmException, IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        writeAip(aip, os);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(os.toByteArray());
        return ChecksumUtils.computeHexChecksum(inputStream, MD5_ALGORITHM);
    }

    @Override
    public Page<AIPEntity> findByFilters(SearchAIPsParameters filters, Pageable pageable) {
        long start = System.currentTimeMillis();
        Page<AIPEntity> response = aipRepository.findAll(new AIPSpecificationsBuilder().withParameters(filters).build(),
                                                         pageable);

        LOGGER.debug("{} AIPS found in  {}ms", response.getSize(), System.currentTimeMillis() - start);
        return response;
    }

    @Override
    public Collection<AIPEntity> findByAipIds(Collection<String> aipIds) {
        return aipRepository.findByAipIdIn(aipIds);
    }

    @Override
    public Set<AIPEntity> findLastByProviderIds(Collection<String> providerIds) {
        return aipRepository.findByProviderIdInAndLast(providerIds, true);
    }

    @Override
    public Optional<String> handleVersioning(AIPEntity aipEntity,
                                             VersioningMode versioningMode,
                                             Map<String, AIPEntity> lastVersions) {

        Optional<String> aipIdToDelete = Optional.empty();
        // lets get the old last version
        AIPEntity dbLatest = lastVersions.get(aipEntity.getProviderId());

        if (dbLatest == null) {
            //then this is the first version (according to our code, not necessarily V1) ingested
            updateLastFlag(aipEntity, true);
            lastVersions.put(aipEntity.getProviderId(), aipEntity);
        } else {
            if (dbLatest.getVersion() < aipEntity.getVersion()) {
                // Switch last entity
                updateLastFlag(dbLatest, false);
                updateLastFlag(aipEntity, true);
                lastVersions.put(aipEntity.getProviderId(), aipEntity);
            } else {
                updateLastFlag(aipEntity, false);
            }

            sessionNotifier.incrementNewProductVersion(aipEntity);
            // In case versioning mode is IGNORE or MANUAL, we do not even reach this point in code
            // In case versioning mode is INC_VERSION, then we have nothing particular to do
            // But in case it is REPLACE...
            if (versioningMode == VersioningMode.REPLACE) {
                sessionNotifier.incrementProductReplace(aipEntity);
                if (aipEntity.isLast()) {
                    // we are the last aip so we need to delete the old latest
                    aipIdToDelete = Optional.of(dbLatest.getAipId());
                } else {
                    //we are not the last aip but we have been added at the same time than the latest, so we need to be removed
                    aipIdToDelete = Optional.of(aipEntity.getAipId());
                }
            }
        }
        return aipIdToDelete;
    }

    @Override
    public void deleteByIds(Collection<String> aipIdsToDelete, SessionDeletionMode mode) {
        Assert.notEmpty(aipIdsToDelete, "list of aips to delete should not be empty");
        OAISDeletionPayloadDto dto = OAISDeletionPayloadDto.build(mode);
        dto.withAipIdsIncluded(aipIdsToDelete);
        oaisDeletionRequestService.registerOAISDeletionCreator(dto);
    }

    @Override
    public Page<AIPEntityLight> findLightByFilters(SearchAIPsParameters filters, Pageable pageable) {
        long start = System.currentTimeMillis();

        Page<AIPEntityLight> response = aipLigthRepository.findAll(new AIPLightSpecificationsBuilder().withParameters(
            filters).build(), pageable);

        LOGGER.debug("{} AIPS found in  {}ms", response.getSize(), System.currentTimeMillis() - start);
        return response;
    }

    @Override
    public List<String> findTags(SearchAIPsParameters filters) {
        return customAIPRepository.getDistinct(AIPQueryGenerator.searchAipTagsUsingSQL(filters));
    }

    @Override
    public List<String> findStorages(SearchAIPsParameters filters) {
        return customAIPRepository.getDistinct(AIPQueryGenerator.searchAipStoragesUsingSQL(filters));
    }

    @Override
    public List<String> findCategories(SearchAIPsParameters filters) {
        return customAIPRepository.getDistinct(AIPQueryGenerator.searchAipCategoriesUsingSQL(filters));
    }

    @Override
    public void downloadAIP(OaisUniformResourceName aipId, HttpServletResponse response) throws ModuleException {

        // Find AIP
        AIPEntity aipEntity = aipRepository.findByAipId(aipId.toString()).orElse(null);
        if (aipEntity == null) {
            String message = String.format("AIP with URN %s not found!", aipId);
            LOGGER.error(message);
            throw new EntityNotFoundException(message);
        }

        AIPDto aip = aipEntity.getAip();

        // Populate response
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + aip.getProviderId() + ".json");
        // NOTE : Do not set content type after download. It can be ignored.
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

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
    private void writeAip(AIPDto aip, OutputStream os) throws IOException {
        Writer osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        JsonWriter writer = new JsonWriter(osw);
        if (aip.getNormalizedGeometry() == null) {
            aip.setNormalizedGeometry(IGeometry.unlocated());
        }
        writer.setIndent(JSON_INDENT);
        gson.toJson(aip, AIPDto.class, writer);
        osw.flush();
        writer.flush();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void registerUpdatesCreator(AIPUpdateParametersDto params) {
        AIPUpdatesCreatorRequest request = AIPUpdatesCreatorRequest.build(params);
        request = (AIPUpdatesCreatorRequest) requestService.scheduleRequest(request);
        if (request.getState() != InternalRequestState.BLOCKED) {
            requestService.scheduleJob(request);
        }
    }

    @Override
    public Optional<AIPEntity> getAip(OaisUniformResourceName aipId) {
        return aipRepository.findByAipId(aipId.toString());
    }

    @Override
    public Set<AIPEntity> findBySipId(String sipId) {
        return aipRepository.findBySipSipId(sipId);
    }

    @Override
    public List<AIPEntity> saveAll(Collection<AIPEntity> entities) {
        for (AIPEntity aip : entities) {
            aip.setLastUpdate(OffsetDateTime.now());
        }
        return aipRepository.saveAll(entities);
    }
}
