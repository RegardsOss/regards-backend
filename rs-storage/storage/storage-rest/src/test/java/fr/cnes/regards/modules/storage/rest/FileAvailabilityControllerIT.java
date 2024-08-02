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
package fr.cnes.regards.modules.storage.rest;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.fileaccess.dto.availability.FilesAvailabilityRequestDto;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Thomas GUILLOU
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_availability_gdh_it" })
@ActiveProfiles(value = { "default", "test", "noscheduler", "nojobs" }, inheritProfiles = false)
public class FileAvailabilityControllerIT extends AbstractRegardsIT {

    private static final String AVAILABILITY_ENDPOINT = "/"
                                                        + FileDownloadController.DOWNLOAD_RESOURCE_PATH
                                                        + FileDownloadController.STATUS_AVAILABILITY_PATH;

    @Test
    public void test_availability_endpoint() {
        Set<String> checksums = IntStream.range(0, 95)
                                         .mapToObj(i -> UUID.randomUUID().toString())
                                         .collect(Collectors.toSet());
        FilesAvailabilityRequestDto filesAvailabilityRequestDto = new FilesAvailabilityRequestDto(checksums);
        performDefaultPost(AVAILABILITY_ENDPOINT,
                           filesAvailabilityRequestDto,
                           customizer().expectStatus(HttpStatus.OK),
                           "Endpoint should return OK status");
        checksums = IntStream.range(0, 105).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toSet());
        filesAvailabilityRequestDto = new FilesAvailabilityRequestDto(checksums);
        performDefaultPost(AVAILABILITY_ENDPOINT,
                           filesAvailabilityRequestDto,
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY).expectToHaveSize("$.messages", 1),
                           "Endpoint should return a status 422 with an error message");
    }
}
