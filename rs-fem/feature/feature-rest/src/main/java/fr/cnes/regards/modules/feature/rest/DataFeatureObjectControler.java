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
package fr.cnes.regards.modules.feature.rest;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.service.IDataObjectFeatureService;

/**
 * End point to get {@link DataObjectFeature} contain data of the last created/modified {@link FeatureEntity}
 * @author Kevin Marchois
 *
 */
@RestController
@RequestMapping(DataFeatureObjectControler.PATH_DATA_FEATURE_OBJECT)
public class DataFeatureObjectControler {

    public static final String PATH_DATA_FEATURE_OBJECT = "dataObjectFeature";

    @Autowired
    private IDataObjectFeatureService dataObjectFeature;

    /**
     * Get a {@link Page} of {@link DataObjectFeature} it will containt data of the last created {@link FeatureEntity}
     * @param model model of wanted {@link Feature}
     * @param lastUpdateDate las modification date that we want {@link Feature}
     * @return {@link RequestInfo} a {@link Page} of {@link DataObjectFeature}
     */
    @RequestMapping(method = RequestMethod.GET, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_VALUE)
    @ResourceAccess(description = "Public a feature and return the request id")
    public ResponseEntity<Page<DataObjectFeature>> createFeatures(@RequestParam("model") String model,
            @RequestParam("lastUpdateDate") OffsetDateTime lastUpdateDate, Pageable page) {

        return new ResponseEntity<Page<DataObjectFeature>>(dataObjectFeature.findAll(model, page, lastUpdateDate),
                HttpStatus.OK);
    }
}
