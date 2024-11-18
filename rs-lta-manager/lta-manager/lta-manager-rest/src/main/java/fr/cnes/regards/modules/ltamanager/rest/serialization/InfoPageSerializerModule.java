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
package fr.cnes.regards.modules.ltamanager.rest.serialization;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionInfoPageDto;
import org.springframework.hateoas.PagedModel;

/**
 * Custom Jackson Module to serialize {@link SessionInfoPageDto} which is a subclass of {@link PagedModel}.
 * This module must be specified in META-INF/services/com.fasterxml.jackson.databind.Module to be actualy used by
 * jackson
 *
 * @author Thibaud Michaudel
 */
public class InfoPageSerializerModule extends SimpleModule {

    @Override
    public void setupModule(Module.SetupContext context) {
        super.setupModule(context);
        context.addBeanSerializerModifier(new BeanSerializerModifier() {

            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                      BeanDescription desc,
                                                      JsonSerializer<?> serializer) {
                // Note that jackson uses the most specific serializer available for a given type
                if (SessionInfoPageDto.class.isAssignableFrom(desc.getBeanClass())) {
                    return new SerializerSessionInfoPage<>();
                }
                return serializer;
            }
        });
    }

}
