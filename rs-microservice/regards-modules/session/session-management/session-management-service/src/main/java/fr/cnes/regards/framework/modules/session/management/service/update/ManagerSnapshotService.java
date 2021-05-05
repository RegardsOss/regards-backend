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
package fr.cnes.regards.framework.modules.session.management.service.update;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.management.dao.ISessionRepository;
import fr.cnes.regards.framework.modules.session.management.dao.ISourceRepository;
import fr.cnes.regards.framework.modules.session.management.domain.AggregationState;
import fr.cnes.regards.framework.modules.session.management.domain.DeltaSessionStep;
import fr.cnes.regards.framework.modules.session.management.domain.ManagerState;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import fr.cnes.regards.framework.modules.session.management.domain.Source;
import fr.cnes.regards.framework.modules.session.management.domain.SourceStepAggregation;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class ManagerSnapshotService {

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private ISessionRepository sessionRepo;

    @Autowired
    private ISourceRepository sourceRepo;

    @Value("${regards.session.management.session.step.page.size:1000}")
    private int sessionStepPageSize;

    /**
     * Create or update the session and source snapshots
     *
     * @param snapshotProcess process used to calculate the sessionSteps to be processed
     * @param freezeDate      corresponds to the scheduler date. Every sessionStep with a lastUpdateDate after the
     *                        freezeDate will not be processed.
     */
    public void generateSnapshots(SnapshotProcess snapshotProcess, OffsetDateTime freezeDate) {
        // Session snapshot
        Set<Session> sessionSet = new HashSet<>();
        // Source snapshot
        String sourceName = snapshotProcess.getSource();
        Source source = this.sourceRepo.findByName(sourceName).orElse(new Source(sourceName));

        // Calculate sessions and sources aggregations
        boolean interrupted = calculateSnapshots(sessionSet, source, sourceName, snapshotProcess.getLastUpdateDate(),
                                                 freezeDate);

        if (!interrupted && !sessionSet.isEmpty()) {
            this.sessionRepo.saveAll(sessionSet);
            this.sourceRepo.save(source);
        } else {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Calculate session and aggregation snapshots
     *
     * @param sessionSet
     * @param source
     * @param sourceName
     * @param lastUpdateDate
     * @param freezeDate
     * @return
     */
    private boolean calculateSnapshots(Set<Session> sessionSet, Source source, String sourceName,
            OffsetDateTime lastUpdateDate, OffsetDateTime freezeDate) {
        // Map sessionName - session to calculate sessions snapshots
        Map<String, Session> sessionMap = new HashMap<>();
        // Map stepType - agg to calculate source snapshot
        Map<StepTypeEnum, SourceStepAggregation> aggByStep = source.getSteps().stream()
                .collect(Collectors.toMap(SourceStepAggregation::getType, s -> s));

        // CREATE SESSIONS
        boolean interrupted;
        Pageable pageToRequest = PageRequest.of(0, sessionStepPageSize, Sort.by(Sort.Order.asc("id")));
        // iterate on all pages of sessionSteps to create Sessions
        do {
            interrupted = Thread.currentThread().isInterrupted();
            pageToRequest = updateOnePageSessionSteps(sessionMap, aggByStep, source, sourceName, lastUpdateDate,
                                                      freezeDate, pageToRequest);
        } while (pageToRequest != null && !interrupted);

        // UPDATE SOURCE SNAPSHOT
        if (!interrupted) {
            sessionSet.addAll(sessionMap.values());
            Set<SourceStepAggregation> aggSet = new HashSet<>(aggByStep.values());
            updateSourceProperties(source, aggSet);
        }

        // CHECK IF THREAD WAS INTERRUPTED
        interrupted = Thread.currentThread().isInterrupted();
        return interrupted;
    }

    /**
     * Update sessions and src aggregation with one page of new session steps
     *
     * @param sessionMap
     * @param aggByStep
     * @param source
     * @param sourceName
     * @param lastUpdateDate
     * @param freezeDate
     * @param pageToRequest
     * @return
     */
    private Pageable updateOnePageSessionSteps(Map<String, Session> sessionMap,
            Map<StepTypeEnum, SourceStepAggregation> aggByStep, Source source, String sourceName,
            OffsetDateTime lastUpdateDate, OffsetDateTime freezeDate, Pageable pageToRequest) {

        // Get page of sessionSteps to handle between lastUpdateDate and freezeDate or under freezeDate if
        // lastUpdateDate is null
        Page<SessionStep> sessionStepPage;
        if (lastUpdateDate != null) {
            sessionStepPage = this.sessionStepRepo
                    .findBySourceAndLastUpdateDateBetween(sourceName, lastUpdateDate, freezeDate, pageToRequest);
        } else {
            sessionStepPage = this.sessionStepRepo
                    .findBySourceAndLastUpdateDateBefore(sourceName, freezeDate, pageToRequest);
        }

        // Iterate on each session step retrieve
        List<SessionStep> sessionSteps = sessionStepPage.getContent();
        for (SessionStep sessionStep : sessionSteps) {
            // initialize delta object which represents the difference between the previous version sessionStep (if it
            // exists) and the updated version
            DeltaSessionStep delta = new DeltaSessionStep(sessionStep.getType());
            // update the session and calculate the delta
            updateSession(sessionMap, sourceName, sessionStep, delta);
            // update the source aggregation according to the delta
            updateSourceAgg(aggByStep, source, delta);
        }

        return sessionStepPage.hasNext() ? sessionStepPage.nextPageable() : null;
    }

    /**
     * Create or update a session according to the SessionStep processed
     *
     * @param sessionMap
     * @param sourceName
     * @param sessionStep
     * @param delta
     */
    private void updateSession(Map<String, Session> sessionMap, String sourceName, SessionStep sessionStep,
            DeltaSessionStep delta) {
        String sessionName = sessionStep.getSession();
        // add session if not already in the map
        Session session = sessionMap.get(sessionName);
        if (session == null) {
            // check if session is already present in the database
            Optional<Session> sessionOpt = this.sessionRepo.findBySourceAndName(sourceName, sessionName);

            // if present, get the session
            if (sessionOpt.isPresent()) {
                session = sessionOpt.get();
            } else {
                // if not create a new session
                session = new Session(sourceName, sessionName);
                delta.setSessionAdded(true);
            }
        }
        updateSessionProperties(session, sessionStep, delta);

        // CREATE OR UPDATE SESSION
        sessionMap.put(sessionName, session);
    }

    /**
     * Update properties related to a session and calculate the difference between the previous sessionStep processed
     * (if it exists) and the new one with updated values
     *
     * @param session
     * @param sessionStep
     * @param delta
     */
    private void updateSessionProperties(Session session, SessionStep sessionStep, DeltaSessionStep delta) {
        // UPDATE THE SESSION STEP LINKED TO THE SESSION
        Set<SessionStep> steps = session.getSteps();
        // calculate the difference between the previous sessionStep (if existing) and the new one
        calculateDelta(steps, sessionStep, delta);
        // create or update with the new session step
        steps.add(sessionStep);

        // UPDATE STATES
        // reset session state and calculate the new state
        session.setManagerState(new ManagerState());
        for (SessionStep step : steps) {
            if (step.getState().getErrors() > 0) {
                session.getManagerState().setErrors(true);
            }
            if (step.getState().getWaiting() > 0) {
                session.getManagerState().setWaiting(true);
            }
            if (step.getState().getRunning() > 0) {
                session.getManagerState().setRunning(true);
            }
        }

        // UPDATE SESSION LAST UPDATE DATE
        OffsetDateTime sessionStepLastUpdate = sessionStep.getLastUpdateDate();
        if (sessionStepLastUpdate != null && (session.getLastUpdateDate() == null || session.getLastUpdateDate()
                .isBefore(sessionStepLastUpdate))) {
            session.setLastUpdateDate(sessionStepLastUpdate);
            delta.setLastUpdateDate(sessionStepLastUpdate);
        }
    }

    private void calculateDelta(Set<SessionStep> steps, SessionStep sessionStep, DeltaSessionStep delta) {
        // check if the session is already in the set of session step
        SessionStep oldSessionStep = steps.stream().filter(s -> s.getId() == sessionStep.getId()).findFirst()
                .orElse(null);
        // if present calculate the difference between the previous sessionStep and its update
        if (oldSessionStep != null) {
            // update in/out
            delta.setIn(sessionStep.getInputRelated() - oldSessionStep.getInputRelated());
            delta.setOut(sessionStep.getOutputRelated() - oldSessionStep.getOutputRelated());
            // update states
            delta.setError(sessionStep.getState().getErrors() - oldSessionStep.getState().getErrors());
            delta.setWaiting(sessionStep.getState().getWaiting() - oldSessionStep.getState().getWaiting());
            delta.setRunning(sessionStep.getState().getRunning() - oldSessionStep.getState().getRunning());
            // remove oldSessionStep from the set
            steps.remove(oldSessionStep);
        } else {
            // if oldSessionStep is not present, set the values as is
            // update in/out
            delta.setIn(sessionStep.getInputRelated());
            delta.setOut(sessionStep.getOutputRelated());
            // update states
            delta.setError(sessionStep.getState().getErrors());
            delta.setWaiting(sessionStep.getState().getWaiting());
            delta.setRunning(sessionStep.getState().getRunning());
        }
    }

    /**
     * Update source aggregations by step type
     *
     * @param aggByStep
     * @param source
     * @param deltaStep
     */
    private void updateSourceAgg(Map<StepTypeEnum, SourceStepAggregation> aggByStep, Source source,
            DeltaSessionStep deltaStep) {
        // CREATE AGGREGATION BY TYPE IF NOT EXISTING
        StepTypeEnum stepType = deltaStep.getType();
        SourceStepAggregation agg = aggByStep.get(stepType);
        if (agg == null) {
            agg = new SourceStepAggregation(stepType);
        }

        // UPDATE AGG WITH NEW DELTA VALUES
        agg.setTotalIn(agg.getTotalIn() + deltaStep.getIn());
        agg.setTotalOut(agg.getTotalOut() + deltaStep.getOut());
        AggregationState aggState = agg.getState();
        aggState.setErrors(aggState.getErrors() + deltaStep.getError());
        aggState.setWaiting(aggState.getWaiting() + deltaStep.getWaiting());
        aggState.setRunning(aggState.getRunning() + deltaStep.getRunning());

        // UPDATE SOURCE
        // update number of sessions if one was added
        if (deltaStep.isSessionAdded()) {
            source.setNbSessions(source.getNbSessions() + 1);
        }
        // update lastUpdateDate only if deltaLastUpdate is after
        OffsetDateTime deltaStepLastUpdate = deltaStep.getLastUpdateDate();
        if (deltaStepLastUpdate != null && (source.getLastUpdateDate() == null || source.getLastUpdateDate()
                .isBefore(deltaStepLastUpdate))) {
            source.setLastUpdateDate(deltaStepLastUpdate);
        }

        // UPDATE AGGREGATION BY TYPE
        aggByStep.put(stepType, agg);
    }

    /**
     * Update source properties
     *
     * @param source
     * @param aggSet
     */
    private void updateSourceProperties(Source source, Set<SourceStepAggregation> aggSet) {
        source.setSteps(aggSet);
        // reset source state
        source.setManagerState(new ManagerState());
        for (SourceStepAggregation agg : aggSet) {
            if (agg.getState().getErrors() > 0) {
                source.getManagerState().setErrors(true);
            }
            if (agg.getState().getWaiting() > 0) {
                source.getManagerState().setWaiting(true);
            }
            if (agg.getState().getRunning() > 0) {
                source.getManagerState().setRunning(true);
            }
        }

    }
}