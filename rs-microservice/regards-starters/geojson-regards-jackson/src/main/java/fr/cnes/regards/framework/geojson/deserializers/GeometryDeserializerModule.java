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
package fr.cnes.regards.framework.geojson.deserializers;

import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.cnes.regards.framework.geojson.geometry.AbstractGeometry;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;

/**
 * Configure not concret Geometry classes for the mapper Jackson
 *
 * @author Thomas GUILLOU
 **/
public class GeometryDeserializerModule extends SimpleModule {

    public GeometryDeserializerModule() {
        // use IGeometry Jackson Deserialization
        this.addDeserializer(IGeometry.class, new DeserializerIGeometry());
        // need to use also AbstractGeometry Deserializer because of class GeometryCollection:
        // GeometryCollection implements IGeometry but contains a list of AbstractCollection,
        // and not a list of IGeometry.
        this.addDeserializer(AbstractGeometry.class, new GenericGeometryDeserializer<>());
    }
}