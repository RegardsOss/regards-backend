/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.request.update;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Léo Mieulet
 */
@Entity(name = RequestTypeConstant.AIP_UPDATES_CREATOR_VALUE)

public class AIPUpdatesCreatorRequest extends AbstractRequest {

    /**
     * request configuration
     */
    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(JsonBinaryType.class)
    private AIPUpdateParametersDto config;

    /**
     * This is a no-args constructor for jpa, don't use it
     */
    protected AIPUpdatesCreatorRequest() {
    }

    public AIPUpdatesCreatorRequest(String correlationId) {
        super(correlationId);
    }

    public AIPUpdateParametersDto getConfig() {
        return config;
    }

    public void setConfig(AIPUpdateParametersDto config) {
        this.config = config;
    }

    public static AIPUpdatesCreatorRequest build(AIPUpdateParametersDto params) {
        AIPUpdatesCreatorRequest result = new AIPUpdatesCreatorRequest(UUID.randomUUID().toString());
        result.setCreationDate(OffsetDateTime.now());
        result.setState(InternalRequestState.TO_SCHEDULE);
        result.setConfig(params);
        result.setDtype(RequestTypeConstant.AIP_UPDATES_CREATOR_VALUE);
        return result;
    }
}
