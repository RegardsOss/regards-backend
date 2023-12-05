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
package fr.cnes.regards.modules.ingest.dto.sip;

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.geojson.AbstractFeatureCollection;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * SIP collection representation based on GeoJson standard structure.
 *
 * @author Marc Sordi
 */
public class SIPCollection extends AbstractFeatureCollection<SIPDto> {

    @Valid
    @NotNull(message = "Ingest metadata is required")
    private IngestMetadataDto metadata;

    public IngestMetadataDto getMetadata() {
        return metadata;
    }

    public void setMetadata(IngestMetadataDto metadata) {
        this.metadata = metadata;
    }

    /**
     * Create a new {@link SIPCollection} with a generated unique request id.<br/>
     * An {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     *
     * @param metadata metadata built with {@link IngestMetadataDto#build(String, String, String, java.util.Set, fr.cnes.regards.modules.ingest.domain.sip.VersioningMode, String, java.util.List)}
     * @return a {@link SIPCollection}
     */
    public static SIPCollection build(IngestMetadataDto metadata) {
        SIPCollection collection = new SIPCollection();
        collection.setMetadata(metadata);
        return collection;
    }
}
