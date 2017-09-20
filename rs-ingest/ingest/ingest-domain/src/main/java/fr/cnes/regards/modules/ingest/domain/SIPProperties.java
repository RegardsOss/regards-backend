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

import java.util.ArrayList;
import java.util.List;

/**
 * GeoJson SIP properties
 *
 * @author Marc Sordi
 *
 */
public class SIPProperties {

    /**
     * Tag list
     */
    private List<String> tags = new ArrayList<>();

    private final List<SIPDataObject> dataObjects = new ArrayList<>();

    // TODO add extra properties

    public List<SIPDataObject> getDataObjects() {
        return dataObjects;
    }

    public void addDataObject(SIPDataObject dataObject) {
        dataObjects.add(dataObject);
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }
}
