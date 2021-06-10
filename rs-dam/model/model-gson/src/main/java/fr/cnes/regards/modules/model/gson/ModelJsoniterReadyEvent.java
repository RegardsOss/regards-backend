package fr.cnes.regards.modules.model.gson;

import org.springframework.context.ApplicationEvent;

public class ModelJsoniterReadyEvent extends ApplicationEvent {

    public ModelJsoniterReadyEvent(Object source) {
        super(source);
    }

}
