/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.geojson.modules;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import fr.cnes.regards.framework.geojson.deserializers.DeserializerIGeometry;
import fr.cnes.regards.framework.geojson.deserializers.GenericGeometryDeserializer;
import fr.cnes.regards.framework.geojson.geometry.AbstractGeometry;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.serializers.SerializerIGeometry;

/**
 * Configure not concrete Geometry classes for the mapper Jackson
 *
 * @author Thomas GUILLOU
 **/
public class GeometrySerializerModule extends SimpleModule {

    public GeometrySerializerModule() {
        // use IGeometry Jackson Deserialization
        super.addDeserializer(IGeometry.class, new DeserializerIGeometry<>());
        // need to use also AbstractGeometry Deserializer because of class GeometryCollection:
        // GeometryCollection implements IGeometry but contains a list of AbstractCollection,
        // and not a list of IGeometry.
        super.addDeserializer(AbstractGeometry.class, new GenericGeometryDeserializer<>());
    }

    @Override
    public void setupModule(Module.SetupContext context) {
        super.setupModule(context);
        context.addBeanSerializerModifier(new BeanSerializerModifier() {

            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                      BeanDescription desc,
                                                      JsonSerializer<?> serializer) {
                if (IGeometry.class.isAssignableFrom(desc.getBeanClass())) {
                    return new SerializerIGeometry((JsonSerializer<Object>) serializer);
                }
                return serializer;
            }
        });
    }
}
