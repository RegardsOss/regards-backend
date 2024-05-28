/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.request.dissemination;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.dissemination.AIPDisseminationRequestDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * An ingest request of type AIP_DISSEMINATION_CREATOR.
 * An AipDisseminationCreatorJob is created by AipDisseminationCreatorRequest
 *
 * @author Thomas GUILLOU
 **/
@Entity(name = RequestTypeConstant.AIP_DISSEMINATION_CREATOR_VALUE)
public class AipDisseminationCreatorRequest extends AbstractRequest {

    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private AIPDisseminationRequestDto request;

    public static AipDisseminationCreatorRequest build(AIPDisseminationRequestDto disseminationRequestDto) {
        AipDisseminationCreatorRequest creator = new AipDisseminationCreatorRequest();
        creator.setRequest(disseminationRequestDto);
        creator.setDtype(RequestTypeConstant.AIP_DISSEMINATION_CREATOR_VALUE);
        creator.setCreationDate(OffsetDateTime.now());
        creator.setState(InternalRequestState.TO_SCHEDULE);
        
        return creator;
    }

    public AIPDisseminationRequestDto getRequest() {
        return request;
    }

    public void setRequest(AIPDisseminationRequestDto request) {
        this.request = request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AipDisseminationCreatorRequest that = (AipDisseminationCreatorRequest) o;
        return Objects.equals(request, that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request);
    }

    @Override
    public String toString() {
        return "AipDisseminationRequestCreator{" + "request=" + request + '}';
    }
}