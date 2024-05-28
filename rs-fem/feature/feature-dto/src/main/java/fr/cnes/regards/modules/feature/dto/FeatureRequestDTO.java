/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.dto;

import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * DTO to provides information about feature requests.
 *
 * @author Sébastien Binda
 */
public class FeatureRequestDTO {

    /**
     * Virtual attribute name cotaining containing request provider id if defined.
     */
    public static final String PROVIDER_ID_FIELD_NAME = "providerId";

    public static final String SESSION_FIELD_NAME = "session";

    public static final String SOURCE_FIELD_NAME = "source";

    /**
     * Request identifier
     */
    @NotNull
    private Long id;

    /**
     * URN of the associated feature if any
     */
    private FeatureUniformResourceName urn;

    /**
     * Provider of the associated feature if any
     */
    private String providerId;

    /**
     * State of the request
     */
    @NotNull
    private RequestState state;

    private FeatureRequestStep step;

    /**
     * Does the request currently processed ?
     */
    private boolean processing = false;

    /**
     * Request registration date
     */
    @NotNull
    private OffsetDateTime registrationDate;

    /**
     * Request type
     */
    private String type;

    /**
     * Source of the feature request.
     */
    private String source;

    /**
     * Session of the feature request
     */
    private String session;

    /**
     * List of error messages
     */
    private Set<String> errors;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public RequestState getState() {
        return state;
    }

    public void setState(RequestState state) {
        this.state = state;
    }

    public OffsetDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(OffsetDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isProcessing() {
        return processing;
    }

    public void setProcessing(boolean processing) {
        this.processing = processing;
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

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public void setStep(FeatureRequestStep step) {
        this.step = step;
    }

    public FeatureRequestStep getStep() {
        return step;
    }

}
