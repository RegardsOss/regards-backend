package fr.cnes.regards.modules.storage.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

public class MockEventHandler implements IHandler<AIPEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MockEventHandler.class);

    private final List<AIPEvent> receivedEvents = new ArrayList<>();

    @Override
    public void handle(TenantWrapper<AIPEvent> wrapper) {
        LOG.info("[MOCK EVENT HANDLER] New AIPEvent Recieved- {} - {}", wrapper.getContent().getAipState().toString(),
                 wrapper.getContent().getAipId());
        int size = receivedEvents.size();
        receivedEvents.add(wrapper.getContent());
        if (receivedEvents.size() == size) {
            LOG.warn("Event already received");
        }
    }

    public List<AIPEvent> getReceivedEvents() {
        return receivedEvents;
    }

    public void clear() {
        receivedEvents.clear();
    }

    public void log(Gson gson) {
        receivedEvents.forEach(event -> LOG.info("Received event : ipId:{}, state:{}. GSON={}", event.getAipId(),
                                                 event.getAipState(), gson.toJson(event)));
    }

}
