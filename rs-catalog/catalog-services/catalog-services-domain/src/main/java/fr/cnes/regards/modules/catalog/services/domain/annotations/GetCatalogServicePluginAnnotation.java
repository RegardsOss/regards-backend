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
package fr.cnes.regards.modules.catalog.services.domain.annotations;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.function.Function;

/**
 * Function returning the {@link CatalogServicePlugin} annotation on given {@link PluginConfiguration}
 *
 * @author Xavier-Alexandre Brochard
 */
public class GetCatalogServicePluginAnnotation implements Function<PluginConfiguration, CatalogServicePlugin> {

    @Override
    public CatalogServicePlugin apply(PluginConfiguration pPluginConfiguration) {
        try {
            return AnnotationUtils.findAnnotation(Class.forName(pPluginConfiguration.getPluginClassName()),
                                                  CatalogServicePlugin.class);
        } catch (ClassNotFoundException e) {
            // No exception should occurs there. If any occurs it should set the application into maintenance mode so we
            // can safely rethrow as a runtime
            throw new PluginUtilsRuntimeException("Could not instanciate plugin", e);
        }
    }

}
