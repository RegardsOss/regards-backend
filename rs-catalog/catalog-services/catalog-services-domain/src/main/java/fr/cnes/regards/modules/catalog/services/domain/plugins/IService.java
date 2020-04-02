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
package fr.cnes.regards.modules.catalog.services.domain.plugins;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;

/**
 * Plugin applying processus according to its parameters.
 *
 * @author Sébastien Binda
 */
@PluginInterface(description = "Plugin applying processus on a query")
public interface IService {

    /**
     * Default method to apply a catalog service plugin. Check the type of plugin service and call the asscoiated method if any
     * @param pParameters plugin parameters {@link ServicePluginParameters}
     * @return @{link ResponseEntity}
     */
    public default ResponseEntity<StreamingResponseBody> apply(ServicePluginParameters pParameters,
            HttpServletResponse response) {
        if ((this instanceof ISingleEntityServicePlugin) && (pParameters.getEntityId() != null)) {
            return ((ISingleEntityServicePlugin) this).applyOnEntity(pParameters.getEntityId(), response);
        } else if (this instanceof IEntitiesServicePlugin) {
            if (pParameters.getSearchRequest() != null) {
                return ((IEntitiesServicePlugin) this).applyOnQuery(pParameters.getSearchRequest(),
                                                                    pParameters.getEntityType(), response);
            } else {
                return ((IEntitiesServicePlugin) this).applyOnEntities(pParameters.getEntitiesId(), response);
            }
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

}
