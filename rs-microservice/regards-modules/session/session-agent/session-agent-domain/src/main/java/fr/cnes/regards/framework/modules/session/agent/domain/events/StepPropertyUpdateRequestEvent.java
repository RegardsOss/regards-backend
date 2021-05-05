package fr.cnes.regards.framework.modules.session.agent.domain.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;

/**
 * Events sent to update {@link SessionStep}.
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class StepPropertyUpdateRequestEvent implements ISubscribable {

    /**
     * Creation date
     */
    private OffsetDateTime date;

    /**
     * Type of event indicating how to modify the property value
     */
    private StepPropertyEventTypeEnum type;

    /**
     * Step Property
     */
    private StepProperty stepProperty;


    public StepPropertyUpdateRequestEvent(StepProperty stepProperty, StepPropertyEventTypeEnum type) {
        this.stepProperty = stepProperty;
        this.type = type;
        this.date = OffsetDateTime.now();
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public StepPropertyEventTypeEnum getType() {
        return type;
    }

    public void setType(StepPropertyEventTypeEnum type) {
        this.type = type;
    }

    public StepProperty getStepProperty() {
        return stepProperty;
    }

    public void setStepProperty(StepProperty stepProperty) {
        this.stepProperty = stepProperty;
    }
}