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

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import fr.cnes.regards.framework.geojson.deserializers.DeserializerMimeType;
import fr.cnes.regards.framework.geojson.serializers.SerializerMimeType;
import org.springframework.util.MimeType;

/**
 * Module to help Jackson serialize and deserialize spring MimeType
 *
 * @author Sébastien Binda
 **/
public class MimeTypeSerializerModule extends SimpleModule {

    @Override
    public void setupModule(Module.SetupContext context) {
        super.setupModule(context);
        context.addBeanSerializerModifier(new BeanSerializerModifier() {

            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                      BeanDescription desc,
                                                      JsonSerializer<?> serializer) {
                if (MimeType.class.isAssignableFrom(desc.getBeanClass())) {
                    return new SerializerMimeType();
                }
                return serializer;
            }
        });
        context.addBeanDeserializerModifier(new BeanDeserializerModifier() {

            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                          BeanDescription beanDesc,
                                                          JsonDeserializer<?> deserializer) {
                if (MimeType.class.isAssignableFrom(beanDesc.getBeanClass())) {
                    return new DeserializerMimeType();
                }
                return deserializer;
            }
        });
    }

}
