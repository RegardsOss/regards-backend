/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPSessionRepository;
import fr.cnes.regards.modules.ingest.dao.SIPEntitySpecifications;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;
import fr.cnes.regards.modules.storage.domain.RejectedSip;

/**
 * Service to handle access to {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class SIPService implements ISIPService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPService.class);

    @Autowired
    private IPublisher publisher;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPSessionRepository sipSessionRepository;

    @Override
    public Page<SIPEntity> search(String providerId, String sessionId, String owner, OffsetDateTime from,
            List<SIPState> state, String processing, Pageable page) {
        return sipRepository
                .findAll(SIPEntitySpecifications.search(providerId, sessionId, owner, from, state, processing), page);
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
    public Collection<RejectedSip> deleteSIPEntitiesBySipIds(Collection<UniformResourceName> sipIds)
            throws ModuleException {
        List<String> sipIdsStr = new ArrayList<>();
        if (sipIds != null) {
            sipIds.forEach(sipId -> sipIdsStr.add(sipId.toString()));
        }
        return this.deleteSIPEntities(sipRepository.findBySipIdIn(sipIdsStr));
    }

    @Override
    public Collection<RejectedSip> deleteSIPEntitiesForProviderId(String providerId) throws ModuleException {
        return this.deleteSIPEntities(sipRepository.findAllByProviderIdOrderByVersionAsc(providerId));
    }

    @Override
    public Collection<RejectedSip> deleteSIPEntitiesForSessionId(String sessionId) throws ModuleException {
        return this.deleteSIPEntities(sipRepository.findBySessionId(sessionId));
    }

    @Override
    public Collection<RejectedSip> deleteSIPEntities(Collection<SIPEntity> sips) throws ModuleException {
        Set<RejectedSip> undeletableSips = Sets.newHashSet();
        long sipDeletionCheckStart = System.currentTimeMillis();
        for (SIPEntity sip : sips) {
            if (isDeletableWithAIPs(sip)) {
                // If SIP is not stored, we can already delete sip and associated AIPs
                if (isDeletableWithoutAips(sip)) {
                    Set<AIPEntity> aips = aipRepository.findBySip(sip);
                    if (!aips.isEmpty()) {
                        aipRepository.delete(aips);
                    }
                    sipRepository.updateSIPEntityState(SIPState.DELETED, sip.getId());
                } else {
                    sipRepository.updateSIPEntityState(SIPState.TO_BE_DELETED, sip.getId());
                }
            } else {
                String errorMsg = String.format("SIPEntity with state %s is not deletable", sip.getState());
                undeletableSips.add(new RejectedSip(sip.getSipId().toString(), errorMsg));
                LOGGER.error(errorMsg);
            }
        }
        long sipDeletionCheckEnd = System.currentTimeMillis();
        LOGGER.debug("Checking {} sips for deletion took {} ms", sips.size(),
                     sipDeletionCheckEnd - sipDeletionCheckStart);
        return undeletableSips;
    }

    @Override
    public Collection<SIPEntity> getAllVersions(String providerId) {
        return sipRepository.getAllVersions(providerId);
    }

    @Override
    public Boolean isDeletable(UniformResourceName sipId) throws EntityNotFoundException {
        Optional<SIPEntity> os = sipRepository.findOneBySipId(sipId.toString());
        if (os.isPresent()) {
            return isDeletableWithAIPs(os.get());
        } else {
            throw new EntityNotFoundException(sipId.toString(), SIPEntity.class);
        }
    }

    @Override
    public SIPEntity saveSIPEntity(SIPEntity sip) {
        // do save SIP
        SIPEntity savedSip = sipRepository.save(sip);
        // Update associated session
        SIPSession session = savedSip.getSession();
        session.setLastActivationDate(OffsetDateTime.now());
        session = sipSessionRepository.save(session);
        savedSip.setSession(session);
        publisher.publish(new SIPEvent(savedSip));
        return savedSip;
    }

    /**
     * Check if the given {@link SIPEntity} is in a state that allow to start a deletion process.
     * In this case the deletion process have to wait for other microservice deletion results to change
     * {@link SIPEntity} state
     * to deleted.
     * @param sip {@link SIPEntity} to check for deletion
     * @return
     */
    private boolean isDeletableWithAIPs(SIPEntity sip) {
        switch (sip.getState()) {
            case CREATED:
            case AIP_CREATED:
            case INVALID:
            case AIP_GEN_ERROR:
            case REJECTED:
            case STORED:
            case SUBMISSION_ERROR:
            case STORE_ERROR:
            case INCOMPLETE:
            case INDEXED:
            case INDEX_ERROR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if the given {@link SIPEntity} is in a state that allow to delete it directly without waiting for deletion
     * confirmation
     * of other microservices.
     * @param sip {@link SIPEntity} to check for deletion.
     * @return
     */
    private boolean isDeletableWithoutAips(SIPEntity sip) {
        switch (sip.getState()) {
            case CREATED:
            case AIP_CREATED:
            case INVALID:
            case AIP_GEN_ERROR:
            case REJECTED:
                return true;
            default:
                return false;
        }
    }
}
