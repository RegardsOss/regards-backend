/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.dto.availability.FilesAvailabilityRequestDto;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Thomas GUILLOU
 **/
@ActiveProfiles(value = { "default", "test", "noscheduler", "nojobs" }, inheritProfiles = false)
@TestPropertySource(locations = { "classpath:test.properties" },
                    properties = { "spring.jpa.properties.hibernate.default_schema=file_availability_rest" })
public class FileAvailabilityControllerIT extends AbstractRegardsIT {

    @Test
    public void test_files_availabilities_endpoint_empty() {
        String url = FileAvailabilityController.AVAILABILITY_ROOT_PATH
                     + FileAvailabilityController.AVAILABILITY_OF_PRODUCTS_PATH;
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of());
        
        performDefaultPost(url, request, customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY), "");
    }

    @Test
    public void test_files_availabilities_endpoint_too_many_elements() {
        String url = FileAvailabilityController.AVAILABILITY_ROOT_PATH
                     + FileAvailabilityController.AVAILABILITY_OF_PRODUCTS_PATH;
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(IntStream.range(0, 101)
                                                                                       .mapToObj(t -> buildURN())
                                                                                       .map(Objects::toString)
                                                                                       .collect(Collectors.toSet()));

        performDefaultPost(url, request, customizer().expectStatus(HttpStatus.BAD_REQUEST), "");
    }

    @Test
    public void test_files_availabilities_endpoint_product_unknown() {
        String url = FileAvailabilityController.AVAILABILITY_ROOT_PATH
                     + FileAvailabilityController.AVAILABILITY_OF_PRODUCT_PATH;

        performDefaultGet(url, customizer().expectStatus(HttpStatus.INTERNAL_SERVER_ERROR), "", buildURN().toString());
    }

    private UniformResourceName buildURN() {
        return UniformResourceName.build("id", EntityType.DATA, "tenant", UUID.randomUUID(), 1);
    }
}
