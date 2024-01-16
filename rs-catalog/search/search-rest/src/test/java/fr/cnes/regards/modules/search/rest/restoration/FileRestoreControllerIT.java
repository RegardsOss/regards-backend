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
package fr.cnes.regards.modules.search.rest.restoration;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.dto.restoration.FilesRestoreRequestDto;
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
 * @author Stephane Cortine
 */
@ActiveProfiles(value = { "default", "test", "noscheduler", "nojobs" }, inheritProfiles = false)
@TestPropertySource(locations = { "classpath:test.properties" },
                    properties = { "spring.jpa.properties.hibernate.default_schema=file_restore_rest" })
public class FileRestoreControllerIT extends AbstractRegardsIT {

    @Test
    public void test_files_restore_endpoint_empty() {
        String url = FileRestoreController.ROOT_PATH;
        FilesRestoreRequestDto filesRestoreRequestDto = new FilesRestoreRequestDto(Set.of());

        performDefaultPost(url, filesRestoreRequestDto, customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY), "");
    }

    @Test
    public void test_files_restore_endpoint_too_many_elements() {
        String url = FileRestoreController.ROOT_PATH;
        FilesRestoreRequestDto filesRestoreRequestDto = new FilesRestoreRequestDto(IntStream.range(0, 101)
                                                                                            .mapToObj(t -> buildURN())
                                                                                            .map(Objects::toString)
                                                                                            .collect(Collectors.toSet()));

        performDefaultPost(url, filesRestoreRequestDto, customizer().expectStatus(HttpStatus.BAD_REQUEST), "");
    }

    @Test
    public void test_files_restore_endpoint_product_unknown() {
        String url = FileRestoreController.ROOT_PATH;
        FilesRestoreRequestDto filesRestoreRequestDto = new FilesRestoreRequestDto(Set.of(buildURN().toString()));

        performDefaultPost(url,
                           filesRestoreRequestDto,
                           customizer().expectStatus(HttpStatus.INTERNAL_SERVER_ERROR),
                           "");
    }

    @Test
    public void test_file_restore_endpoint_product_unknown() {
        String url = FileRestoreController.ROOT_PATH + FileRestoreController.PRODUCT_ID_RELATIVE_PATH;

        performDefaultGet(url, customizer().expectStatus(HttpStatus.INTERNAL_SERVER_ERROR), "", buildURN().toString());
    }

    private UniformResourceName buildURN() {
        return UniformResourceName.build("id", EntityType.DATA, "tenant", UUID.randomUUID(), 1);
    }
}
