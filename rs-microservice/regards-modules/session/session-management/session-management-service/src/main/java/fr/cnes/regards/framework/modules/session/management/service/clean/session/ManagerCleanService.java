package fr.cnes.regards.framework.modules.session.management.service.clean.session;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.management.dao.ISessionManagerRepository;
import fr.cnes.regards.framework.modules.session.management.dao.ISourceManagerRepository;
import fr.cnes.regards.framework.modules.session.management.domain.DeltaSessionStep;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import fr.cnes.regards.framework.modules.session.management.domain.Source;
import fr.cnes.regards.framework.modules.session.management.domain.SourceStepAggregation;
import java.time.OffsetDateTime;
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
import org.springframework.stereotype.Service;

/**
 * Service to clean old {@link SessionStep} and {@link Session}.
 * Clean also {@link Source} with no sessions associated
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class ManagerCleanService {

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private ISessionManagerRepository sessionRepo;

    @Autowired
    private ISourceManagerRepository sourceRepo;

    @Value("${regards.session.manager.clean.session.limit.store.session:30}")
    private int limitStoreSessionSteps;

    @Value("${regards.session.manager.clean.session.page:1000}")
    private int pageSize;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerCleanService.class);

    /**
     * The clean method performs three actions :
     * - delete old session steps and sessions (all items with lastUpdateDates before a configured date)
     * - update source aggregation information due to the session removals.
     * - delete sources which are not linked to any sessions
     * @return number of sessions deleted
     */
    public int clean() {
        // Init startClean with the current date minus the limit of SessionStep save configured
        OffsetDateTime startClean = OffsetDateTime.now().minusDays(this.limitStoreSessionSteps);
        LOGGER.debug("Check old session steps and sessions before {}", startClean);

        // find all sessions to be deleted
        int nbSessions = 0;
        Pageable page = PageRequest.of(0, pageSize, Sort.by("id"));
        Page<Session> sessionPage;

        Map<String, Source> sourceMap = new HashMap<>();

        do {
            sessionPage = this.sessionRepo.findByLastUpdateDateBefore(startClean, page);
            List<Session> sessionList = sessionPage.getContent();
            // update source aggregation information due to session removal
            updateSource(sourceMap, sessionList);
            // delete expired sessions
            this.sessionRepo.deleteInBatch(sessionPage);
            nbSessions += sessionList.size();
        } while (sessionPage.hasNext());


        // save all changes on sources
        this.sourceRepo.saveAll(sourceMap.values());

        // delete source not associated to any sessions
        this.sourceRepo.deleteByNbSessions(0);

        // delete expired session steps
        this.sessionStepRepo.deleteByLastUpdateDateBefore(startClean);

        return nbSessions;
    }

    /**
     * Update source information due to the session removals
     * @param sourceMap map of sourceName - source
     * @param sessionList list of sessions related to a source
     */
    private void updateSource(Map<String, Source> sourceMap, List<Session> sessionList) {
        // create a map to handle sessions by source
        Map<String, List<Session>> sessionBySource = sessionList.stream()
                .collect(Collectors.groupingBy(Session::getSource));

        // iterate on all sessions of the source
        for (Map.Entry<String, List<Session>> entry : sessionBySource.entrySet()) {
            String sourceName = entry.getKey();
            // retrieve the source from the database
            Source source = sourceMap
                    .computeIfAbsent(sourceName, value -> this.sourceRepo.findByName(sourceName).orElse(null));
            // if a source was found, update its related information
            if (source != null) {
                List<Session> sessionListBySource = entry.getValue();
                Map<StepTypeEnum, DeltaSessionStep> deltaByType = new EnumMap<>(StepTypeEnum.class);
                for (Session session : sessionListBySource) {
                    source.setNbSessions(source.getNbSessions() - 1);
                    updateDelta(session.getSteps(), deltaByType);
                }
                updateSourceAgg(source, deltaByType);

                sourceMap.put(sourceName, source);
            }
        }
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