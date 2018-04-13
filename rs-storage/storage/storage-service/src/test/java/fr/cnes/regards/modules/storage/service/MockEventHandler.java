package fr.cnes.regards.modules.storage.service;

import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

public class MockEventHandler implements IHandler<AIPEvent> {

    private final Set<AIPEvent> receivedEvents = Sets.newHashSet();

    @Override
    public void handle(TenantWrapper<AIPEvent> wrapper) {
        receivedEvents.add(wrapper.getContent());
    }

    public Set<AIPEvent> getReceivedEvents() {
        return receivedEvents;
    }

    public void clear() {
        receivedEvents.clear();
    }

}
