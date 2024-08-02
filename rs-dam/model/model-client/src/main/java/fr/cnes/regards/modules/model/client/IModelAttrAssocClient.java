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
package fr.cnes.regards.modules.model.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@RestClient(name = "rs-dam", contextId = "rs-dam.model-att-assoc.client")
public interface IModelAttrAssocClient {

    /**
     * Client base path
     */
    String BASE_MAPPING = "/models";

    /**
     * Client type path
     */
    String TYPE_MAPPING = "/{modelName}/attributes";

    /**
     * Client association path
     */
    String ASSOCS_MAPPING = "/assocs";

    @GetMapping(path = BASE_MAPPING + TYPE_MAPPING)
    ResponseEntity<List<EntityModel<ModelAttrAssoc>>> getModelAttrAssocs(@PathVariable("modelName") String modelName);

    /**
     * Retrieve model attribute associations for a given entity type (optional)
     *
     * @return the model attribute associations
     */
    @GetMapping(path = BASE_MAPPING + ASSOCS_MAPPING)
    ResponseEntity<Collection<ModelAttrAssoc>> getModelAttrAssocsFor(
        @RequestParam(name = "type", required = false) EntityType type);

}
