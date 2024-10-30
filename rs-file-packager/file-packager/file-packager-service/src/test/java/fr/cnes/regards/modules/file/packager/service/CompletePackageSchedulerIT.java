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
 * Test for {@link fr.cnes.regards.modules.file.packager.service.scheduler.CompletePackageScheduler}
 */
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=complete_package_scheduler_test" })
public class CompletePackageSchedulerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private PackageReferenceRepository packageReferenceRepository;

    @Autowired
    private FilePackagerService filePackagerService;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException, URISyntaxException, IOException {
        packageReferenceRepository.deleteAll();
    }

    @Test
    public void test_schedule_complete_package() {
        // GIVEN
        PackageReference incompletePackage = new PackageReference("storage", "subdir1");
        incompletePackage.setStatus(PackageReferenceStatus.BUILDING);
        incompletePackage = packageReferenceRepository.save(incompletePackage);

        PackageReference completePackage = new PackageReference("storage", "subdir1");
        completePackage.setStatus(PackageReferenceStatus.TO_STORE);
        completePackage = packageReferenceRepository.save(completePackage);

        // WHEN
        filePackagerService.scheduleStoreCompletePackageJobs();

        // THEN
        Optional<PackageReference> oPackage = packageReferenceRepository.findById(incompletePackage.getId());
        Assertions.assertTrue(oPackage.isPresent(), "The incomplete package should still be there");
        Assertions.assertEquals(PackageReferenceStatus.BUILDING,
                                oPackage.get().getStatus(),
                                "The incomplete package should still be in BUILDING status");

        oPackage = packageReferenceRepository.findById(completePackage.getId());
        Assertions.assertTrue(oPackage.isPresent(), "The complete package should still be there");
        Assertions.assertEquals(PackageReferenceStatus.STORE_IN_PROGRESS,
                                oPackage.get().getStatus(),
                                "The incomplete package should now be in STORE_IN_PROGRESS status");
    }
}
