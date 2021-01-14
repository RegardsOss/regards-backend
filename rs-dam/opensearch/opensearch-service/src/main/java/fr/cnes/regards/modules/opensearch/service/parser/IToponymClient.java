package fr.cnes.regards.modules.opensearch.service.parser;/*
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

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Client interface for requesting the toponym service
 * @author Iliana Ghazali
 *
 * //FIXME : Move this class to the new toponym service
 */

@RestClient(name = "rs-dam", contextId = "rs-dam.toponym.client")
@RequestMapping(value = IToponymClient.PATH_TOPONYMS, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface IToponymClient {

    String PATH_TOPONYMS = "/toponyms";

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    ResponseEntity<EntityModel<IGeometry>> retrieveToponym(
            @RequestParam(name = "businessId", required = false) String businessId);

}
