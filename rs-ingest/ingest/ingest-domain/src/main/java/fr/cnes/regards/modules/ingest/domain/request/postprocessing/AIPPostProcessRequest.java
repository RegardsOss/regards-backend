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


package fr.cnes.regards.modules.ingest.domain.request.postprocessing;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request to postprocess aips
 *
 * @author Iliana Ghazali
 */
@Entity(name = RequestTypeConstant.AIP_POST_PROCESS_VALUE)
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class AIPPostProcessRequest extends AbstractRequest {

    /**
     * request configuration
     */
    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private AIPPostProcessPayload config;

    /**
     * AIP to process
     */
    @ManyToOne
    @JoinColumn(name = "aip_id",
                referencedColumnName = "id",
                foreignKey = @ForeignKey(name = "fk_postprocessing_request_aip"))
    private AIPEntity aip;

    public AIPPostProcessRequest(String correlationId) {
        super(correlationId);
    }

    /**
     * This is a no-args constructor for jpa, don't use it
     */
    protected AIPPostProcessRequest() {
    }

    public static AIPPostProcessRequest build(AIPEntity aipToProcess, String postProcessingPluginBusinessId) {
        AIPPostProcessRequest appr = new AIPPostProcessRequest(UUID.randomUUID().toString());
        appr.aip = aipToProcess;
        appr.config = AIPPostProcessPayload.build(postProcessingPluginBusinessId);
        appr.setCreationDate(OffsetDateTime.now());
        appr.setSessionOwner(aipToProcess.getSessionOwner());
        appr.setSession(aipToProcess.getSession());
        appr.setProviderId(aipToProcess.getProviderId());
        appr.setDtype(RequestTypeConstant.AIP_POST_PROCESS_VALUE);
        appr.setState(InternalRequestState.TO_SCHEDULE);
        return appr;
    }

    public AIPPostProcessPayload getConfig() {
        return config;
    }

    public AIPEntity getAip() {
        return aip;
    }

}
