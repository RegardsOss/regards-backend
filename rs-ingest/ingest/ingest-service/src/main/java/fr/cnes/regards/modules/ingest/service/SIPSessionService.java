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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPSessionRepository;
import fr.cnes.regards.modules.ingest.dao.SIPSessionSpecifications;
import fr.cnes.regards.modules.ingest.domain.builder.SIPSessionBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.storage.domain.RejectedSip;

@Service
@MultitenantTransactional
public class SIPSessionService implements ISIPSessionService {

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private ISIPSessionRepository sipSessionRepository;

    @Autowired
    private ISIPService sipService;

    public static final String DEFAULT_SESSION_ID = "default";

    @Override
    public SIPSession getSession(String sessionId, Boolean createIfNotExists) {
        SIPSession session = null;
        String id = sessionId;
        if (sessionId == null) {
            id = DEFAULT_SESSION_ID;
        }
        Optional<SIPSession> oSession = sipSessionRepository.findById(id);
        if (oSession.isPresent()) {
            session = oSession.get();
        } else if (createIfNotExists) {
            session = sipSessionRepository.save(SIPSessionBuilder.build(id));
        }
        return session;
    }

    @Override
    public Page<SIPSession> search(String id, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Page<SIPSession> pagedSessions = sipSessionRepository
                .findAll(SIPSessionSpecifications.search(id, from, to), pageable);
        List<SIPSession> sessions = Lists.newArrayList();
        pagedSessions.forEach(s -> sessions.add(this.addSessionSipInformations(s)));
        return new PageImpl<>(sessions, pageable, pagedSessions.getTotalElements());
    }

    /**
     * Create a {@link SIPSession} for the session id.
     * @return {@link SIPSession}
     */
    private SIPSession addSessionSipInformations(SIPSession session) {
        long sipsCount = sipRepository.countBySessionId(session.getId());
        long indexedSipsCount = sipRepository
                .countBySessionIdAndStateIn(session.getId(), Sets.newHashSet(SIPState.INDEXED));
        long storedSipsCount = sipRepository.countBySessionIdAndStateIn(session.getId(),
                                                                        Sets.newHashSet(SIPState.STORED,
                                                                                        SIPState.INDEXED,
                                                                                        SIPState.INDEX_ERROR));
        long generatedSipsCount = sipRepository.countBySessionIdAndStateIn(session.getId(),
                                                                           Sets.newHashSet(SIPState.AIP_CREATED,
                                                                                           SIPState.STORED,
                                                                                           SIPState.INDEXED,
                                                                                           SIPState.INDEX_ERROR,
                                                                                           SIPState.INCOMPLETE,
                                                                                           SIPState.STORE_ERROR));
        long errorSipsCount = sipRepository.countBySessionIdAndStateIn(session.getId(),
                                                                       Sets.newHashSet(SIPState.AIP_GEN_ERROR,
                                                                                       SIPState.REJECTED,
                                                                                       SIPState.STORE_ERROR,
                                                                                       SIPState.INVALID,
                                                                                       SIPState.INDEX_ERROR));
        long deletedSipsCount = sipRepository
                .countBySessionIdAndStateIn(session.getId(), Sets.newHashSet(SIPState.DELETED));
        session.setErrorSipsCount(errorSipsCount);
        session.setGeneratedSipsCount(generatedSipsCount);
        session.setIndexedSipsCount(indexedSipsCount);
        session.setStoredSipsCount(storedSipsCount);
        session.setSipsCount(sipsCount);
        session.setDeletedSipsCount(deletedSipsCount);
        return session;
    }

    @Override
    public Collection<RejectedSip> deleteSIPSession(String id) throws ModuleException {
        return sipService.deleteSIPEntitiesForSessionId(id);
    }

}
