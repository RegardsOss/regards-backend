package fr.cnes.regards.framework.domain;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;

import java.time.OffsetDateTime;

/**
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class StepEvent implements ISubscribable {

    private String stepId;

    private String source;

    private String session;

    private OffsetDateTime date;

    private StepTypeEnum stepType;

    private StepEventStateEnum state;

    private String property;

    private String value;

    private EventTypeEnum eventTypeEnum;

    private boolean input_related;

    private boolean output_related;


    public StepEvent(String stepId, String source, String session, StepTypeEnum stepType, StepEventStateEnum state,
            String property
            , String value, EventTypeEnum eventTypeEnum, boolean input_related, boolean output_related) {
        this.source = source;
        this.session = session;
        this.date = OffsetDateTime.now();
        this.stepType = stepType;
        this.state = state;
        this.property = property;
        this.value = value;
        this.eventTypeEnum = eventTypeEnum;
        this.input_related = input_related;
        this.output_related = output_related;
    }


    public String getStepId() {
        return stepId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public StepTypeEnum getStepType() {
        return stepType;
    }

    public void setStepType(StepTypeEnum stepType) {
        this.stepType = stepType;
    }

    public StepEventStateEnum getState() {
        return state;
    }

    public void setState(StepEventStateEnum state) {
        this.state = state;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public EventTypeEnum getEventTypeEnum() {
        return eventTypeEnum;
    }

    public void setEventTypeEnum(EventTypeEnum eventTypeEnum) {
        this.eventTypeEnum = eventTypeEnum;
    }

    public boolean isInput_related() {
        return input_related;
    }

    public void setInput_related(boolean input_related) {
        this.input_related = input_related;
    }

    public boolean isOutput_related() {
        return output_related;
    }

    public void setOutput_related(boolean output_related) {
        this.output_related = output_related;
    }
}
