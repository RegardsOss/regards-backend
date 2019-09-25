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

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.ingest.dao.IStorageDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.StorageDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
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
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.SIPEntitySpecifications;
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
    private IStorageDeletionRequestRepository storageDeletionRequest;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Override
    public Page<SIPEntity> search(String providerId, String sessionOwner, String session, OffsetDateTime from,
            List<SIPState> state, String ingestChain, Pageable page) {
        return sipRepository
                .loadAll(SIPEntitySpecifications.search(providerId == null ? null : Sets.newHashSet(providerId),
                                                        null, sessionOwner, session, from, state, ingestChain,
                        true, null, null, null, page),
                         page);
    }

    @Override
    public SIPEntity getEntity(String sipId) throws EntityNotFoundException {
        Optional<SIPEntity> sipEntity = sipRepository.findOneBySipId(sipId.toString());
        if (sipEntity.isPresent()) {
            return sipEntity.get();
        } else {
            throw new EntityNotFoundException(sipEntity.toString(), SIPEntity.class);
        }
    }

    @Override
    public void scheduleDeletion(SIPEntity sipEntity, SessionDeletionMode deletionMode) {
        // Update AIPs state as deleted and retrieve events to delete associated files and AIPs
        String deleteRequestId = aipService.scheduleAIPEntityDeletion(sipEntity.getSipId());

        sessionNotifier.notifySIPDeleting(sipEntity);
        sipEntity.setState(SIPState.DELETED);
        save(sipEntity);

        // Save the request id sent to storage
        StorageDeletionRequest sdr = StorageDeletionRequest.build(deleteRequestId, sipEntity.getSipId(), deletionMode);
        storageDeletionRequest.save(sdr);
    }

    @Override
    public void processDeletion(String sipId, boolean deleteIrrevocably) {
        Optional<SIPEntity> optionalSIPEntity = sipRepository.findOneBySipId(sipId);
        if (optionalSIPEntity.isPresent()) {
            SIPEntity sipEntity = optionalSIPEntity.get();
            sessionNotifier.notifySIPDeleted(sipEntity);
            if (!deleteIrrevocably) {
                // Mark the SIP correctly deleted
                sipEntity.setState(SIPState.DELETED);
                sipEntity.setErrors(null);
                save(sipEntity);
            } else {
                sipRepository.delete(sipEntity);
            }
        }
    }

    @Override
    public boolean validatedVersionExists(String providerId) {
        return sipRepository.countByProviderIdAndStateIn(providerId) > 0;
    }

    @Override
    public SIPEntity save(SIPEntity sip) {
        // update last update
        sip.setLastUpdate(OffsetDateTime.now());
        // save
        return sipRepository.save(sip);
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
