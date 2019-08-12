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
package fr.cnes.regards.modules.ingest.service;

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

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.SIPEntitySpecifications;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.entity.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

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

    public static final String SESSION_NOTIF_STEP = "SIP";

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Gson gson;

    @Autowired
    private ISIPRepository sipRepository;

    @Override
    public Page<SIPEntity> search(String providerId, String sessionOwner, String session, OffsetDateTime from,
            List<SIPState> state, String ingestChain, Pageable page) {
        return sipRepository
                .loadAll(SIPEntitySpecifications.search(providerId, sessionOwner, session, from, state, ingestChain),
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

    //    @Override
    //    public Collection<RejectedSipDto> deleteSIPEntitiesBySipIds(Collection<UniformResourceName> sipIds)
    //            throws ModuleException {
    //        List<String> sipIdsStr = new ArrayList<>();
    //        if (sipIds != null) {
    //            sipIds.forEach(sipId -> sipIdsStr.add(sipId.toString()));
    //        }
    //        return this.deleteSIPEntities(sipRepository.findBySipIdIn(sipIdsStr));
    //    }
    //
    //    @Override
    //    public Collection<RejectedSipDto> deleteSIPEntitiesForProviderId(String providerId) throws ModuleException {
    //        return this.deleteSIPEntities(sipRepository.findAllByProviderIdOrderByVersionAsc(providerId));
    //    }
    //
    //    @Override
    //    public Collection<RejectedSipDto> deleteSIPEntitiesForSession(String sessionOwner, String session)
    //            throws ModuleException {
    //        return this.deleteSIPEntities(sipRepository
    //                .findByIngestMetadataSessionOwnerAndIngestMetadataSession(sessionOwner, session));
    //    }

    //    @Override
    //    public Collection<RejectedSipDto> deleteSIPEntities(Collection<SIPEntity> sips) throws ModuleException {
    //        Set<RejectedSipDto> undeletableSips = Sets.newHashSet();
    //        long sipDeletionCheckStart = System.currentTimeMillis();
    //        for (SIPEntity sip : sips) {
    //            if (isDeletableWithAIPs(sip)) {
    //                // If SIP is not stored, we can already delete sip and associated AIPs
    //                if (isDeletableWithoutAips(sip)) {
    //                    Set<AIPEntity> aips = aipRepository.findBySip(sip);
    //                    if (!aips.isEmpty()) {
    //                        aipRepository.deleteAll(aips);
    //                    }
    //                    notifySipChangedState(sip.getIngestMetadata(), sip.getState(), SIPState.DELETED);
    //                    sipRepository.updateSIPEntityState(SIPState.DELETED, sip.getId());
    //                } else {
    //                    notifySipChangedState(sip.getIngestMetadata(), sip.getState(), SIPState.TO_BE_DELETED);
    //                    sipRepository.updateSIPEntityState(SIPState.TO_BE_DELETED, sip.getId());
    //                }
    //            } else if (sip.getState() != SIPState.TO_BE_DELETED && sip.getState() != SIPState.DELETED) {
    //                // We had this condition on those state here and not into #isDeletableWithAIPs because we just want to be silent.
    //                // Indeed, if we ask for deletion of an already deleted or being deleted SIP that just mean there is less work to do this time.
    //                String errorMsg = String.format("SIPEntity with state %s is not deletable", sip.getState());
    //                undeletableSips.add(new RejectedSipDto(sip.getSipId().toString(), errorMsg));
    //                LOGGER.error(errorMsg);
    //            }
    //        }
    //        long sipDeletionCheckEnd = System.currentTimeMillis();
    //        LOGGER.debug("Checking {} sips for deletion took {} ms", sips.size(),
    //                     sipDeletionCheckEnd - sipDeletionCheckStart);
    //        return undeletableSips;
    //    }

    @Override
    public Collection<SIPEntity> getAllVersions(String providerId) {
        return sipRepository.getAllVersions(providerId);
    }

    @Override
    public boolean validatedVersionExists(String providerId) {
        return sipRepository.countByProviderIdAndStateIn(providerId) > 0;
    }

    //    @Override
    //    public Boolean isDeletable(UniformResourceName sipId) throws EntityNotFoundException {
    //        Optional<SIPEntity> os = sipRepository.findOneBySipId(sipId.toString());
    //        if (os.isPresent()) {
    //            return isDeletableWithAIPs(os.get());
    //        } else {
    //            throw new EntityNotFoundException(sipId.toString(), SIPEntity.class);
    //        }
    //    }

    @Override
    public SIPEntity saveSIPEntity(SIPEntity sip) {
        // do save SIP
        SIPEntity savedSip = sipRepository.save(sip);
        publisher.publish(new SIPEvent(savedSip));
        return savedSip;
    }

    @Override
    public void notifySipChangedState(IngestMetadata metadata, SIPState previousState, SIPState nextState) {
        notifySipsChangedState(metadata, previousState, nextState, 1);
    }

    @Override
    public void notifySipsChangedState(IngestMetadata metadata, SIPState previousState, SIPState nextState, int nbSip) {

        // Decrement the previous state by one
        publisher.publish(SessionMonitoringEvent
                .build(metadata.getSessionOwner(), metadata.getSession(), SessionNotificationState.OK,
                       SESSION_NOTIF_STEP, SessionNotificationOperator.DEC, previousState.toString(), nbSip));

        // Increment the next state by one
        publisher.publish(SessionMonitoringEvent.build(metadata.getSessionOwner(), metadata.getSession(),
                                                       SessionNotificationState.OK, SESSION_NOTIF_STEP,
                                                       SessionNotificationOperator.INC, nextState.toString(), nbSip));
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
