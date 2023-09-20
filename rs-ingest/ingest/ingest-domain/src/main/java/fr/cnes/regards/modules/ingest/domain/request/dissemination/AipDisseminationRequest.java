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
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * An ingest request of type AIP_DISSEMINATION.
 * The goal of this request is to keep information about an AIP dissemination.
 * The scheduler AipDisseminationJobScheduler will use one or many requests and associated it to a DisseminationJob.
 *
 * @author Thomas GUILLOU
 **/
@Entity(name = RequestTypeConstant.AIP_DISSEMINATION_VALUE)
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AipDisseminationRequest extends AbstractRequest {

    /**
     * AIP to disseminate
     */
    @ManyToOne
    @JoinColumn(name = "aip_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_update_request_aip"))
    private AIPEntity aip;

    /**
     * List of dissemination destination
     */
    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb",
          parameters = { @org.hibernate.annotations.Parameter(name = JsonTypeDescriptor.ARG_TYPE,
                                                              value = "java.lang.String") })
    private List<String> recipients;

    public static AipDisseminationRequest build(AIPEntity aip, List<String> recipients) {
        AipDisseminationRequest disseminationRequest = new AipDisseminationRequest();
        disseminationRequest.aip = aip;
        disseminationRequest.recipients = recipients;
        disseminationRequest.setCreationDate(OffsetDateTime.now());
        disseminationRequest.setSessionOwner(aip.getSessionOwner());
        disseminationRequest.setSession(aip.getSession());
        disseminationRequest.setProviderId(aip.getProviderId());
        disseminationRequest.setDtype(RequestTypeConstant.AIP_DISSEMINATION_VALUE);
        disseminationRequest.setState(InternalRequestState.TO_SCHEDULE);
        return disseminationRequest;
    }

    public AIPEntity getAip() {
        return aip;
    }

    public void setAip(AIPEntity aip) {
        this.aip = aip;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AipDisseminationRequest that = (AipDisseminationRequest) o;
        return Objects.equals(aip, that.aip) && Objects.equals(recipients, that.recipients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aip, recipients);
    }

    @Override
    public String toString() {
        return "AipDisseminationRequest{" + "aip=" + aip + ", recipients=" + recipients + '}';
    }
}
