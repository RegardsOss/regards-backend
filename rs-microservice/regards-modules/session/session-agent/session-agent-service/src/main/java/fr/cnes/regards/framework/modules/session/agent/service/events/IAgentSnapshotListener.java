package fr.cnes.regards.framework.modules.session.agent.service.events;

import fr.cnes.regards.framework.modules.session.agent.domain.StepEvent;
import fr.cnes.regards.framework.modules.session.agent.service.jobs.AgentSnapshotJobService;

import java.util.List;

/**
 * @author Iliana Ghazali
 **/
public interface IAgentSnapshotListener {


    void onStepEventAvailable(List<StepEvent> stepEventList);

    void subscribe(AgentSnapshotJobService newSubscriber);

    void unsubscribe(AgentSnapshotJobService unscriber);

}
