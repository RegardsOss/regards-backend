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
package fr.cnes.regards.modules.catalog.services.service;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IEntitiesServicePlugin;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory.CatalogPluginResponseType;
import fr.cnes.regards.modules.catalog.services.plugins.AbstractCatalogServicePlugin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;

@Plugin(description = "Example many plugin.",
        id = "ManyTestPlugin",
        version = "1.0.0",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
@CatalogServicePlugin(applicationModes = { ServiceScope.MANY }, entityTypes = { EntityType.DATA })
public class ExampleManyPlugin extends AbstractCatalogServicePlugin implements IEntitiesServicePlugin {

    @Override
    public ResponseEntity<StreamingResponseBody> apply(ServicePluginParameters pParameters,
                                                       HttpServletResponse response) {
        return CatalogPluginResponseFactory.createSuccessResponse(response,
                                                                  CatalogPluginResponseType.JSON,
                                                                  "Response example !");
    }

}
