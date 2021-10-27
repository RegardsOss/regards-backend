/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.manager.service.clean.session;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.manager.dao.ISessionManagerRepository;
import fr.cnes.regards.framework.modules.session.manager.dao.ISourceManagerRepository;
import fr.cnes.regards.framework.modules.session.manager.domain.DeltaSessionStep;
import fr.cnes.regards.framework.modules.session.manager.domain.Session;
import fr.cnes.regards.framework.modules.session.manager.domain.Source;
import fr.cnes.regards.framework.modules.session.manager.domain.SourceStepAggregation;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;

/**
 * Service to clean old {@link SessionStep} and {@link Session}.
 * Clean also {@link Source} with no sessions associated
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class ManagerCleanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerCleanService.class);

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private ISessionManagerRepository sessionRepo;

    @Autowired
    private ISourceManagerRepository sourceRepo;

    @Autowired
    private ManagerCleanService self;

    @Value("${regards.session.manager.clean.session.limit.store:30}")
    private int limitStoreSessionSteps;

    @Value("${regards.session.manager.clean.session.page:100}")
    private int pageSize;

    /**
     * The clean method performs three actions :
     * - delete old session steps and sessions (all items with lastUpdateDates before a configured date)
     * - update source aggregation information due to the session removals.
     * - delete sources which are not linked to any sessions
     * @return number of sessions deleted
     */
    public int clean() {
        // Init startClean with the current date minus the limit of SessionStep save configured
        OffsetDateTime startClean = OffsetDateTime.now(ZoneOffset.UTC).minusDays(this.limitStoreSessionSteps);
        LOGGER.debug("Check old session steps and sessions before {}", startClean);
        boolean interrupted = Thread.currentThread().isInterrupted();

        // init parameters
        int nbSessions = 0;
        Pageable page = PageRequest.of(0, pageSize, Sort.by("id"));
        int nbSessionsProcessed = 0;

        // find all sessions to be deleted and update the source information (they are related to the sessions)
        do {
            nbSessionsProcessed = self.processOnePage(startClean, page);
            nbSessions += nbSessionsProcessed;
        } while (!interrupted && nbSessionsProcessed != 0);

        // delete source not associated to any sessions
        this.sourceRepo.deleteByNbSessions(0L);
        // delete expired session steps
        this.sessionStepRepo.deleteByLastUpdateDateBefore(startClean);

        // log if thread was interrupted
        if (interrupted) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }
        return nbSessions;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public int processOnePage(OffsetDateTime startClean, Pageable page) {
        // Find a list of sessions outdated
        Page<Session> sessionPage = this.sessionRepo.findByLastUpdateDateBefore(startClean, page);
        // Update source aggregation information due to session removal
        Map<String, Source> updatedSourcesMap = updateSources(sessionPage.getContent());
        // Delete expired sessions
        this.sessionRepo.deleteInBatch(sessionPage);
        // Save all changes on sources
        this.sourceRepo.saveAll(updatedSourcesMap.values());
        return sessionPage.getNumberOfElements();
    }

    /**
     * Update source information due to the session removals
     * @param sessionList list of sessions related to a source
     */
    private Map<String, Source> updateSources(List<Session> sessionList) {
        Map<String, Source> updatedSourcesMap = new HashMap<>();
        Map<String, List<Session>> sessionsRetrievedBySource = sessionList.stream()
                .collect(Collectors.groupingBy(Session::getSource));

        // iterate on all sessions of the source
        for (Map.Entry<String, List<Session>> entry : sessionsRetrievedBySource.entrySet()) {
            String sourceName = entry.getKey();
            // retrieve the source from the database
            Source source = updatedSourcesMap
                    .computeIfAbsent(sourceName, value -> this.sourceRepo.findByName(sourceName).orElse(null));
            // if a source was found, update its related information
            if (source != null) {
                List<Session> sessionListBySource = entry.getValue();
                Map<StepTypeEnum, DeltaSessionStep> deltaByType = new EnumMap<>(StepTypeEnum.class);
                // iterate on all sessions of a source and calculate parameters impacted by the session deletion
                for (Session session : sessionListBySource) {
                    source.setNbSessions(source.getNbSessions() - 1);
                    updateDelta(session.getSteps(), deltaByType);
                }
                // update source with the updated parameters
                updateSourceAgg(source, deltaByType);
                // put updated source in map
                updatedSourcesMap.put(sourceName, source);
            }
        }
        return updatedSourcesMap;
    }

    /**
     * Store the sum of sessionSteps parameters which will impact the source aggregation
     * @param steps sessionSteps of a session
     * @param deltaByType sum of sessionSteps parameters sorted by type of sessionStep
     */
    private void updateDelta(Set<SessionStep> steps, Map<StepTypeEnum, DeltaSessionStep> deltaByType) {
        // iterate on all session steps of the session to update the delta
        for (SessionStep step : steps) {
            StepTypeEnum type = step.getType();
            DeltaSessionStep delta = deltaByType.get(type);

            if (delta == null) {
                delta = new DeltaSessionStep(type);
            }

            // update in/out
            delta.setIn(delta.getIn() + step.getInputRelated());
            delta.setOut(delta.getIn() + step.getOutputRelated());

            // update state
            delta.setError(delta.getError() + step.getState().getErrors());
            delta.setWaiting(delta.getWaiting() + step.getState().getWaiting());
            delta.setRunning(delta.getRunning() + step.getState().getRunning());

            // update delta by type
            deltaByType.put(type, delta);
        }
    }

    /**
     * Update the source aggregation with the calculated delta
     * @param source the source to update
     * @param deltaByType the source step aggregation calculate
     */
    private void updateSourceAgg(Source source, Map<StepTypeEnum, DeltaSessionStep> deltaByType) {
        Set<SourceStepAggregation> aggSet = source.getSteps();
        // iterate on the aggregation steps of the source and update their corresponding values
        for (SourceStepAggregation agg : aggSet) {
            StepTypeEnum type = agg.getType();
            DeltaSessionStep delta = deltaByType.get(type);
            if (delta != null) {
                agg.setTotalIn(agg.getTotalIn() - delta.getIn());
                agg.setTotalOut(agg.getTotalOut() - delta.getOut());
                agg.getState().setErrors(agg.getState().getErrors() - delta.getError());
                agg.getState().setWaiting(agg.getState().getWaiting() - delta.getWaiting());
                agg.getState().setRunning(agg.getState().getRunning() - delta.getRunning());
            }
        }
    }
}