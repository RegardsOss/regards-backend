/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.feature.dto.event.out;

import java.util.Set;

import org.springframework.lang.Nullable;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 * Ingest request event
 *
 * @author Marc SORDI
 *
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FeatureRequestEvent implements ISubscribable {

    /**
     * The requestType
     */
    private FeatureRequestType type;

    /**
     * The request id
     */
    private String requestId;

    /**
     * Owner of the request
     */
    private String requestOwner;

    /**
     * The {@link Feature#getId()}
     */
    private String id;

    /**
     * The feature URN
     */
    private FeatureUniformResourceName urn;

    private RequestState state;

    private Set<String> errors;

    public static FeatureRequestEvent build(FeatureRequestType type, String requestId, String requestOwner,
            @Nullable String id, @Nullable FeatureUniformResourceName urn, RequestState state) {
        return build(type, requestId, requestOwner, id, urn, state, null);
    }

    public static FeatureRequestEvent build(FeatureRequestType type, String requestId, String requestOwner,
            @Nullable String id, @Nullable FeatureUniformResourceName urn, RequestState state, Set<String> errors) {
        FeatureRequestEvent event = new FeatureRequestEvent();
        event.setType(type);
        event.setRequestId(requestId);
        event.setId(id);
        event.setUrn(urn);
        event.setState(state);
        event.setErrors(errors);
        event.setRequestOwner(requestOwner);
        return event;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public String getRequestOwner() {
        return requestOwner;
    }

    public void setRequestOwner(String requestOwner) {
        this.requestOwner = requestOwner;
    }

    public FeatureRequestType getType() {
        return type;
    }

    public void setType(FeatureRequestType type) {
        this.type = type;
    }

}
