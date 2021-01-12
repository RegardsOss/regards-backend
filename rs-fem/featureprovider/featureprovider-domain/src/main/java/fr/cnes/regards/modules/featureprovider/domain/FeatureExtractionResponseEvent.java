package fr.cnes.regards.modules.featureprovider.domain;

import java.util.Set;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FeatureExtractionResponseEvent implements ISubscribable {

    /**
     * This field is just here as to not break compatibility with former version
     */
    private final static String type = FeatureExtractionRequest.REQUEST_TYPE;

    /**
     * The request id
     */
    private String requestId;

    /**
     * Owner of the request
     */
    private String requestOwner;

    private RequestState state;

    private Set<String> errors;

    public FeatureExtractionResponseEvent(String requestId, String requestOwner, RequestState state) {
        this(requestId, requestOwner, state, null);
    }

    public FeatureExtractionResponseEvent(String requestId, String requestOwner, RequestState state,
            Set<String> errors) {
        this.requestId = requestId;
        this.requestOwner = requestOwner;
        this.state = state;
        this.errors = errors;
    }

    public RequestState getState() {
        return state;
    }

    public void setState(RequestState state) {
        this.state = state;
    }

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestOwner() {
        return requestOwner;
    }

    public void setRequestOwner(String requestOwner) {
        this.requestOwner = requestOwner;
    }

    public String getType() {
        return type;
    }
}
