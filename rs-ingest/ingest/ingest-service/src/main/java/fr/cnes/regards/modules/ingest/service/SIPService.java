/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPSessionRepository;
import fr.cnes.regards.modules.ingest.dao.SIPEntitySpecifications;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.storage.client.IAipClient;
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
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPSessionRepository sipSessionRepository;

    @Autowired
    private IAipClient aipClient;

    @Override
    public Page<SIPEntity> getSIPEntities(String sipId, String sessionId, String owner, OffsetDateTime from,
            SIPState state, Pageable page) {
        return sipRepository.findAll(SIPEntitySpecifications.search(sipId, sessionId, owner, from, state), page);
    }

    @Override
    public SIPEntity getSIPEntity(String ipId) throws ModuleException {
        Optional<SIPEntity> sipEntity = sipRepository.findOneByIpId(ipId);
        if (sipEntity.isPresent()) {
            return sipEntity.get();
        } else {
            throw new EntityNotFoundException(ipId, SIPEntity.class);
        }
    }

    @Override
    public Collection<SIPEntity> deleteSIPEntitiesByIpIds(Collection<String> ipIds) throws ModuleException {
        return this.deleteSIPEntities(sipRepository.findByIpIdIn(ipIds));
    }

    @Override
    public Collection<SIPEntity> deleteSIPEntitiesForSipId(String sipId) throws ModuleException {
        return this.deleteSIPEntities(sipRepository.findAllBySipIdOrderByVersionAsc(sipId));
    }

    @Override
    public Collection<SIPEntity> deleteSIPEntitiesForSessionId(String sessionId) throws ModuleException {
        return this.deleteSIPEntities(sipRepository.findBySessionId(sessionId));
    }

    @Override
    public Collection<SIPEntity> deleteSIPEntities(Collection<SIPEntity> sips) throws ModuleException {
        Set<SIPEntity> deletableSips = Sets.newHashSet();
        Set<SIPEntity> undeletableSips = Sets.newHashSet();
        for (SIPEntity sip : sips) {
            if (isDeletableWithAIPs(sip)) {
                // If SIP si not stored, we can already delete sip and associated AIPs
                this.deleteSIPEntity(sip);
                deletableSips.add(sip);
            } else {
                undeletableSips.add(sip);
                LOGGER.error("SIPEntity with state {} is not deletable", sip.getState());
            }
        }
        // Do delete AIPs
        if (!deletableSips.isEmpty()) {
            ResponseEntity<List<RejectedSip>> response = aipClient
                    .deleteAipFromSips(deletableSips.stream().map(SIPEntity::getIpId).collect(Collectors.toSet()));
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                Set<String> rejectedSipIpId = response.getBody().stream().map(r -> r.getSipId())
                        .collect(Collectors.toSet());
                undeletableSips.addAll(deletableSips.stream().filter(d -> rejectedSipIpId.contains(d.getIpId()))
                        .collect(Collectors.toSet()));
            } else {
                undeletableSips.addAll(deletableSips);
            }
        }
        return undeletableSips;
    }

    @Override
    public Collection<SIPEntity> getAllVersions(String sipId) {
        return sipRepository.getAllVersions(sipId);
    }

    @Override
    public Boolean isDeletable(String ipId) throws EntityNotFoundException {
        Optional<SIPEntity> os = sipRepository.findOneByIpId(ipId);
        if (os.isPresent()) {
            return isDeletableWithAIPs(os.get());
        } else {
            throw new EntityNotFoundException(ipId, SIPEntity.class);
        }
    }

    @Override
    public Boolean isRetryable(String ipId) throws EntityNotFoundException {
        Optional<SIPEntity> os = sipRepository.findOneByIpId(ipId);
        if (os.isPresent()) {
            switch (os.get().getState()) {
                case INVALID:
                case AIP_GEN_ERROR:
                    return true;
                default:
                    return false;
            }
        } else {
            throw new EntityNotFoundException(ipId, SIPEntity.class);
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
        return savedSip;
    }

    @Override
    public void deleteSIPEntity(SIPEntity sip) {
        if (isDeletableWithoutAips(sip)) {
            Set<AIPEntity> aips = aipRepository.findBySip(sip);
            if (!aips.isEmpty()) {
                aipRepository.delete(aips);
            }
            sipRepository.updateSIPEntityState(SIPState.DELETED, sip.getId());
        }
    }

    /**
     * Check if the given {@link SIPEntity} is in a state that allow to start a deletion process.
     * In this case the deletion process have to wait for other microservice deletion results to change {@link SIPEntity} state
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
            case STORE_ERROR:
            case INCOMPLETE:
            case INDEXED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if the given {@link SIPEntity} is in a state that allow to delete it directly without waiting for deletion confirmation
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
