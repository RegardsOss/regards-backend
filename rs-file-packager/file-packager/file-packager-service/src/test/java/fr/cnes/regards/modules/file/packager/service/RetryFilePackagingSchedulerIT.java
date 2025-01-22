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
package fr.cnes.regards.modules.file.packager.service;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.modules.file.packager.dao.PackageReferenceRepository;
import fr.cnes.regards.modules.file.packager.domain.PackageReference;
import fr.cnes.regards.modules.file.packager.domain.PackageReferenceStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Test for {@link fr.cnes.regards.modules.file.packager.service.scheduler.RetryFilePackagingScheduler}
 */
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=retry_scheduler_test" })
public class RetryFilePackagingSchedulerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private PackageReferenceRepository packageReferenceRepository;

    @Autowired
    private FilePackagerService filePackagerService;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException, URISyntaxException, IOException {
        packageReferenceRepository.deleteAll();
    }

    @Test
    public void test_retry() {
        // Given
        PackageReference packageReference = new PackageReference("storage", "subdir");
        packageReference.setStatus(PackageReferenceStatus.STORE_ERROR);
        packageReference = packageReferenceRepository.save(packageReference);

        // When
        filePackagerService.retryPackagesInError();

        // Then
        Optional<PackageReference> oPackageReference = packageReferenceRepository.findById(packageReference.getId());
        Assertions.assertTrue(oPackageReference.isPresent(), "The PackageReference should still exsits");
        Assertions.assertEquals(PackageReferenceStatus.TO_STORE,
                                oPackageReference.get().getStatus(),
                                "The PackageReference should have the status TO_STORE");
    }
}
