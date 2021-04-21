/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.featureprovider.domain.plugin;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import fr.cnes.regards.modules.feature.dto.Feature;

/**
 * Generate a {@link Feature} from a {@link FeatureExtractionRequest}
 * @author Kevin Marchois
 */
@PluginInterface(description = "Generate a feature from a reference request")
public interface IFeatureFactoryPlugin {

    /**
     * Generate a {@link Feature} from {@link FeatureExtractionRequest} parameters.
     * @param parameters free extraction parameters
     * @return generated {@link Feature}
     */
    Feature generateFeature(JsonObject parameters) throws ModuleException;
}
