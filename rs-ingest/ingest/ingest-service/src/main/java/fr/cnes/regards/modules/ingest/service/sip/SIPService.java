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
package fr.cnes.regards.modules.ingest.service.sip;

import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.SIPEntitySpecifications;
import fr.cnes.regards.modules.ingest.domain.dto.RejectedAipDto;
import fr.cnes.regards.modules.ingest.domain.dto.RejectedSipDto;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;

/**
 * Service to handle access to {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class SIPService implements ISIPService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SIPService.class);

    public static final String MD5_ALGORITHM = "MD5";

    @Autowired
    private Gson gson;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Override
    public Page<SIPEntity> search(String providerId, String sessionOwner, String session, OffsetDateTime from,
            List<SIPState> state, String ingestChain, Pageable page) {
        return sipRepository
                .loadAll(SIPEntitySpecifications.search(providerId == null ? null : Lists.newArrayList(providerId),
                                                        null, sessionOwner, session, from, state, ingestChain,
                        true, null, null, null),
                         page);
    }

    @Override
    public SIPEntity getSIPEntity(UniformResourceName sipId) throws EntityNotFoundException {
        Optional<SIPEntity> sipEntity = sipRepository.findOneBySipId(sipId.toString());
        if (sipEntity.isPresent()) {
            return sipEntity.get();
        } else {
            throw new EntityNotFoundException(sipEntity.toString(), SIPEntity.class);
        }
    }

    @Override
    public RejectedSipDto deleteSIPEntity(SIPEntity sipEntity, boolean removeIrrevocably) {
        RejectedSipDto rejectedSipDto = null;
        // Remove all files and AIP related to this SIP
        Collection<RejectedAipDto> rejectedAips = aipService.deleteAip(sipEntity.getSipId());
        // Check if all linked AIPs have been removed
        if (rejectedAips.isEmpty()) {
            if (removeIrrevocably) {
                // Completely remove this entity from DB
                sipRepository.delete(sipEntity);
                // Notify the entity does not exist anymore
            } else {
                sipEntity.setState(SIPState.DELETED);
                saveSIPEntity(sipEntity);
            }
            sessionNotifier.notifySIPDeleted(sipEntity);
        } else {
            // TODO collect errors (files not yet stored, etc..)
        }
        return rejectedSipDto;
    }

    @Override
    public boolean validatedVersionExists(String providerId) {
        return sipRepository.countByProviderIdAndStateIn(providerId) > 0;
    }

    @Override
    public SIPEntity saveSIPEntity(SIPEntity sip) {
        // do save SIP
        sip.setLastUpdate(OffsetDateTime.now());
        SIPEntity savedSip = sipRepository.save(sip);
        return savedSip;
    }

    @Override
    public String calculateChecksum(SIP sip) throws NoSuchAlgorithmException, IOException {
        String jsonSip = gson.toJson(sip);
        InputStream inputStream = new ByteArrayInputStream(jsonSip.getBytes());
        return ChecksumUtils.computeHexChecksum(inputStream, MD5_ALGORITHM);
    }

    @Override
    public boolean isAlreadyIngested(String checksum) {
        return sipRepository.isAlreadyIngested(checksum);
    }

    @Override
    public Integer getNextVersion(SIP sip) {
        return sipRepository.getNextVersion(sip.getId());
    }
}
