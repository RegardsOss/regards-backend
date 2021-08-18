package fr.cnes.regards.modules.model.gson.handler;

import fr.cnes.regards.modules.model.gson.ModelGsonReadyEvent;
import fr.cnes.regards.modules.model.gson.ModelJsonReadyEvent;
import fr.cnes.regards.modules.model.gson.ModelJsoniterReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ModelGsonJsoniterReadyListener {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private boolean gsonReady;
    private boolean jsoniterReady;
    private boolean hasFired;

    @EventListener
    public synchronized void handleGsonReady(ModelGsonReadyEvent gsonReadyEvt) {
        if (hasFired) { return; }
        gsonReady = true;
        if (jsoniterReady) { fireReadyEvent(); }
    }

    @EventListener
    public synchronized void handleJsoniterReady(ModelJsoniterReadyEvent jsoniterReadyEvt) {
        if (hasFired) { return; }
        jsoniterReady = true;
        if (gsonReady) { fireReadyEvent(); }
    }

    private void fireReadyEvent() {
        applicationEventPublisher.publishEvent(new ModelJsonReadyEvent(this));
        hasFired = true;
    }

}
