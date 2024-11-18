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
import fr.cnes.regards.framework.geojson.serializers.SerializerPagedModel;
import org.springframework.hateoas.PagedModel;

/**
 * Module to help Jackson serialize and deserialize spring {@link PagedModel} (which is used in api responses)
 *
 * @author Thibaud Michaudel
 **/
public class PageModelSerializerModule extends SimpleModule {

    @Override
    public void setupModule(Module.SetupContext context) {
        super.setupModule(context);
        context.addBeanSerializerModifier(new BeanSerializerModifier() {

            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                      BeanDescription desc,
                                                      JsonSerializer<?> serializer) {
                // Note that jackson uses the most specific serializer available for a given type
                if (PagedModel.class.isAssignableFrom(desc.getBeanClass())) {
                    return new SerializerPagedModel();
                }
                return serializer;
            }
        });
    }
}
