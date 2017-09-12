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

import fr.cnes.regards.framework.geojson.AbstractFeature;

/**
 *
 * SIP representation based on GeoJson standard structure.<br/>
 * GeoJson properties represented by {@link SIPProperties} are used for SIP passed by value.<br/>
 * "ref" extension attribute is used for SIP passed by reference.
 *
 * @author Marc Sordi
 *
 */
public class SIP extends AbstractFeature<SIPProperties, String> {

    /**
     * ref : extension attribute for SIP passed by reference. May be null if SIP passed by value (filling
     * {@link SIPProperties})
     */
    private SIPReference ref;

    public SIP() {
        properties = new SIPProperties();
    }

    public void addDataObject(SIPDataObject dataObject) {
        properties.addDataObject(dataObject);
    }

    public SIPReference getRef() {
        return ref;
    }

    public void setRef(SIPReference ref) {
        this.ref = ref;
    }
}
