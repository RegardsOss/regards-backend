package fr.cnes.regards.framework.modules.session.agent.service.events;

import fr.cnes.regards.framework.modules.session.agent.domain.StepEvent;
import fr.cnes.regards.framework.modules.session.agent.service.jobs.AgentSnapshotJobService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Iliana Ghazali
 **/
@Service
public class AgentSnapshotListenerService implements IAgentSnapshotListener {

    private final Set<AgentSnapshotJobService> subscribers = ConcurrentHashMap.newKeySet();

    public void subscribe(AgentSnapshotJobService newSubscriber) {
        this.subscribers.add(newSubscriber);
    }

    public void unsubscribe(AgentSnapshotJobService subscriber) {
        this.subscribers.remove(subscriber);
    }

    @Override
    public void onStepEventAvailable(List<StepEvent> stepEventList) {
        this.subscribers.forEach(subscriber -> subscriber.handleStepEvents(stepEventList));
    }
}
