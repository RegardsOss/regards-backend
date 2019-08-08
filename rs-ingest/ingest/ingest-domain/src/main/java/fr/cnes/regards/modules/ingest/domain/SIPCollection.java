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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.geojson.AbstractFeatureCollection;
import fr.cnes.regards.modules.ingest.domain.aip.StorageMetadata;

/**
 * SIP collection representation based on GeoJson standard structure.
 *
 * @author Marc Sordi
 *
 */
public class SIPCollection extends AbstractFeatureCollection<SIP> {

    @Valid
    @NotNull(message = "Ingest metadata is required")
    private IngestMetadata metadata;

    public IngestMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(IngestMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Create a new {@link SIPCollection}.
     * @param metadata metadata built with {@link IngestMetadata#build(String, String, String, StorageMetadata...)}
     * @return a {@link SIPCollection}
     */
    public static SIPCollection build(IngestMetadata metadata) {
        SIPCollection collection = new SIPCollection();
        collection.setMetadata(metadata);
        return collection;
    }
}
