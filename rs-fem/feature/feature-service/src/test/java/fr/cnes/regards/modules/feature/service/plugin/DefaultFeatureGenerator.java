/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.plugin;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.plugin.IFeatureFactoryPlugin;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import fr.cnes.regards.modules.model.dto.properties.IProperty;

/**
 * Default plugin notification sender
 * @author Kevin Marchois
 *
 */
@Plugin(author = "REGARDS Team", description = "Default recipient sender", id = "DefaultFeatureGenerator",
        version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class DefaultFeatureGenerator extends AbstractFeatureMultitenantServiceTest implements IFeatureFactoryPlugin {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public Feature generateFeature(JsonObject parameters) {
        String model = mockModelClient("feature_model_01.xml", cps, factory, runtimeTenantResolver.getTenant(),
                                       modelAttrAssocClientMock);
        Feature toAdd = Feature.build(UUID.randomUUID().toString(), "DefaultFeatureGenerator", null,
                                      IGeometry.point(IGeometry.position(10.0, 20.0)), EntityType.DATA, model);
        toAdd.addProperty(IProperty.buildString("data_type", "TYPE01"));
        toAdd.addProperty(IProperty.buildObject("file_characterization",
                                                IProperty.buildBoolean("valid", Boolean.TRUE)));
        return toAdd;
    }

}
