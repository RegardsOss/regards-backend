package fr.cnes.regards.modules.storage.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

public class MockEventHandler implements IHandler<AIPEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MockEventHandler.class);

    private final Set<AIPEvent> receivedEvents = Sets.newHashSet();

    @Override
    public void handle(TenantWrapper<AIPEvent> wrapper) {
        LOG.info("[MOCK EVENT HANDLER] New AIPEvent Recieved- {} - {}", wrapper.getContent().getAipState().toString(),
                 wrapper.getContent().getAipId());
        receivedEvents.add(wrapper.getContent());
    }

    public Set<AIPEvent> getReceivedEvents() {
        return receivedEvents;
    }

    public void clear() {
        receivedEvents.clear();
    }

    public void log() {
        receivedEvents
                .forEach(event -> LOG.info("Received event : ipId:{}, state:{}", event.getAipId(), event.getAipState()));
    }

}
