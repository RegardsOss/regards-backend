package fr.cnes.regards.framework.modules.session.agent.domain.events.update;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
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
     * Unique step identifier
     */
    private String stepId;

    /**
     * Name of the source
     */
    private String source;

    /**
     * Name of the session
     */
    private String session;

    /**
     * Creation date
     */
    private OffsetDateTime date;

    /**
     * Type of event indicating how to modify the property value
     */
    private StepPropertyEventTypeEnum type;

    /**
     * Event information
     */
    private StepPropertyEventInfo stepPropertyEventInfo;

    public StepPropertyUpdateRequestEvent(String stepId, String source, String session, StepPropertyEventTypeEnum type,
            StepPropertyEventInfo stepPropertyEventInfo) {
        this.stepId = stepId;
        this.source = source;
        this.session = session;
        this.date = OffsetDateTime.now();
        this.type = type;
        this.stepPropertyEventInfo = stepPropertyEventInfo;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
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

    public StepPropertyEventTypeEnum getType() {
        return type;
    }

    public void setType(StepPropertyEventTypeEnum type) {
        this.type = type;
    }

    public StepPropertyEventInfo getStepPropertyEventInfo() {
        return stepPropertyEventInfo;
    }

    public void setStepPropertyEventInfo(StepPropertyEventInfo stepPropertyEventInfo) {
        this.stepPropertyEventInfo = stepPropertyEventInfo;
    }
}