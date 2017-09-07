/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.models.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.models.domain.ModelAttrAssoc;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@RestClient(name = "rs-dam")
@RequestMapping(IModelAttrAssocClient.BASE_MAPPING)
public interface IModelAttrAssocClient {

    public static final String BASE_MAPPING = "/models";

    public static final String TYPE_MAPPING = "/{pModelId}/attributes";

    public static final String ASSOCS_MAPPING = "/assocs";

    @RequestMapping(method = RequestMethod.GET, path = TYPE_MAPPING)
    public ResponseEntity<List<Resource<ModelAttrAssoc>>> getModelAttrAssocs(@PathVariable("pModelId") Long pModelId);

    @RequestMapping(path = ASSOCS_MAPPING, method = RequestMethod.GET)
    public ResponseEntity<Collection<ModelAttrAssoc>> getModelAttrAssocsFor(
            @RequestParam(name = "type", required = false) EntityType type);

}
