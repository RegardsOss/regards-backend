/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.geojson.AbstractFeatureCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;

/**
 * SIP collection representation based on GeoJson standard structure.
 *
 * To build a {@link SIPCollection}, you have to use a {@link SIPCollectionBuilder}.
 *
 * @author Marc Sordi
 *
 */
public class SIPCollection extends AbstractFeatureCollection<SIP> {

    private final IngestMetadata metadata;

    public SIPCollection() {
        metadata = new IngestMetadata();
    }

    public IngestMetadata getMetadata() {
        return metadata;
    }
}
