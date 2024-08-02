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
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.search.domain.SearchValidationMappings;
import fr.cnes.regards.modules.search.dto.GeometryRequestParameter;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Sébastien Binda
 **/
@TestPropertySource(locations = "classpath:test.properties",
                    properties = { "spring.jpa.properties.hibernate.default_schema=validation_search_tests" })
@MultitenantTransactional
public class SearchValidationControllerIT extends AbstractRegardsTransactionalIT {

    @Test
    public void test_geometry_validation_ok() {
        performDefaultPost(SearchValidationMappings.TYPE_MAPPING + SearchValidationMappings.GEO_VALIDATION_MAPPING,
                           GeometryRequestParameter.build("POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))"),
                           customizer().expectStatusOk(),
                           "Polygon should be valid");

    }

    @Test
    public void test_geometry_validation_ko() {
        performDefaultPost(SearchValidationMappings.TYPE_MAPPING + SearchValidationMappings.GEO_VALIDATION_MAPPING,
                           GeometryRequestParameter.build("POLYGON((0 0, 0 1, 1 1))"),
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY),
                           "Polygon should not be valid");

        performDefaultPost(SearchValidationMappings.TYPE_MAPPING + SearchValidationMappings.GEO_VALIDATION_MAPPING,
                           GeometryRequestParameter.build("POLYGON((0 0, 0 0, 0 0, 0 0,0 0))"),
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY),
                           "Polygon should not be valid");

        performDefaultPost(SearchValidationMappings.TYPE_MAPPING + SearchValidationMappings.GEO_VALIDATION_MAPPING,
                           GeometryRequestParameter.build("POLYGON((0 0, 0 1, 1 1, 1 0, 0 2))"),
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY),
                           "Polygon should not be valid");

    }
}
