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
package fr.cnes.regards.framework.gson.adapters.sample9;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import fr.cnes.regards.framework.gson.adapters.MultitenantPolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Sébastien Binda
 **/
public class DataAdapterFactory extends MultitenantPolymorphicTypeAdapterFactory {

    protected DataAdapterFactory(IRuntimeTenantResolver tenantResolver) {
        super(tenantResolver, Object.class);
    }

    public void doMapping(Gson gson, String inTenant) {
        super.doMapping(gson, inTenant);
    }

    public ConcurrentMap<String, Map<String, TypeAdapter<?>>> getDiscToDelegateMap() {
        return discToDelegateMap;
    }

    public ConcurrentMap<String, Map<Class<?>, TypeAdapter<?>>> getSubtypeToDelegateMap() {
        return subtypeToDelegateMap;
    }

    public ConcurrentMap<String, Map<String, Class<?>>> getDiscToSubtypeMap() {
        return discToSubtypeMap;
    }

    public ConcurrentMap<String, Map<Class<?>, String>> getSubtypeToDiscMap() {
        return subtypeToDiscMap;
    }
}
