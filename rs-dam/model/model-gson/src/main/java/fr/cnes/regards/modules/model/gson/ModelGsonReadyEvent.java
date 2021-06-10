package fr.cnes.regards.modules.model.gson;

import org.springframework.context.ApplicationEvent;

public class ModelGsonReadyEvent  extends ApplicationEvent {

    public ModelGsonReadyEvent(Object source) {
        super(source);
    }

}
