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
package fr.cnes.regards.modules.feature.domain.plugin;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureReferenceRequest;
import fr.cnes.regards.modules.feature.dto.Feature;

/**
 * Generate a {@link Feature} from a {@link FeatureReferenceRequest}
 * @author Kevin Marchois
 *
 */
@FunctionalInterface
@PluginInterface(description = "Generate a Feature from a FeatureReferenceRequest")
public interface IFeatureFactoryPlugin {

    /**
     * Generate a {@link Feature} from a {@link FeatureReferenceRequest}
     * @param reference {@link FeatureReferenceRequest} data source to generate a {@link Feature}
     * @return generated {@link FeatureCreationRequest}
     */
    Feature createFeature(FeatureReferenceRequest reference) throws ModuleException;

}
