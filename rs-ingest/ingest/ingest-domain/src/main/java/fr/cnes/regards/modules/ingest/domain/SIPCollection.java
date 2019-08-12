/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import fr.cnes.regards.framework.geojson.AbstractFeatureCollection;
import fr.cnes.regards.modules.ingest.domain.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.domain.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.domain.event.IngestRequestEvent;

/**
 * SIP collection representation based on GeoJson standard structure.
 *
 * @author Marc Sordi
 *
 */
public class SIPCollection extends AbstractFeatureCollection<SIP> {

    @NotBlank(message = IngestValidationMessages.MISSING_REQUEST_ID_ERROR)
    @Size(max = 36)
    private String requestId;

    @Valid
    @NotNull(message = "Ingest metadata is required")
    private IngestMetadataDto metadata;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public IngestMetadataDto getMetadata() {
        return metadata;
    }

    public void setMetadata(IngestMetadataDto metadata) {
        this.metadata = metadata;
    }

    /**
     * Create a new {@link SIPCollection} with a generated unique request id.<br/>
     * An {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     * @param metadata metadata built with {@link IngestMetadataDto#build(String, String, String, StorageMetadata...)}
     * @return a {@link SIPCollection}
     */
    public static SIPCollection build(IngestMetadataDto metadata) {
        SIPCollection collection = new SIPCollection();
        collection.setRequestId(UUID.randomUUID().toString());
        collection.setMetadata(metadata);
        return collection;
    }
}
