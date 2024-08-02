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
package fr.cnes.regards.framework.modules.plugins.domain.gson;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.*;

/**
 * Factory to (de)serialize {@link IPluginParam}
 *
 * @author Marc SORDI
 */
@GsonTypeAdapterFactory
public class PluginParamAdapterFactory extends PolymorphicTypeAdapterFactory<IPluginParam> {

    public PluginParamAdapterFactory() {
        super(IPluginParam.class, "type");
        registerSubtype(StringPluginParam.class, PluginParamType.STRING);
        registerSubtype(BytePluginParam.class, PluginParamType.BYTE);
        registerSubtype(ShortPluginParam.class, PluginParamType.SHORT);
        registerSubtype(IntegerPluginParam.class, PluginParamType.INTEGER);
        registerSubtype(LongPluginParam.class, PluginParamType.LONG);
        registerSubtype(FloatPluginParam.class, PluginParamType.FLOAT);
        registerSubtype(DoublePluginParam.class, PluginParamType.DOUBLE);
        registerSubtype(BooleanPluginParam.class, PluginParamType.BOOLEAN);
        registerSubtype(JsonMapPluginParam.class, PluginParamType.MAP);
        registerSubtype(JsonCollectionPluginParam.class, PluginParamType.COLLECTION);
        registerSubtype(JsonObjectPluginParam.class, PluginParamType.POJO);
        registerSubtype(NestedPluginParam.class, PluginParamType.PLUGIN);
        registerSubtype(ModelPluginParam.class, PluginParamType.REGARDS_ENTITY_MODEL);
    }
}
