/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.storage.service.file.repository;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Test some of the basic CRUD method of the repository: of {@link IFileReferenceRepository}.
 *
 * @author Olivier Navarro
 **/
@ActiveProfiles({ "noscheduler", "test" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = fr.cnes.regards.modules.storage.service.file.repository.FileReferenceRequestRepositoryIT.ScanningConfiguration.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileReferenceRepositoryIT extends AbstractDaoTransactionalIT {

    @Autowired
    IFileReferenceRepository repository;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    public static class ScanningConfiguration {

    }

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Before
    @Transactional
    public void create() {
        // clear all since it can happen to have some junk data remaining.
        repository.deleteAll();

        // create a 1st entity
        final FileReference entity1 = newInstance();
        doSave(entity1);

        // create a 2nd entity
        final FileReference entity2 = newInstance();
        entity2.getMetaInfo().setChecksum(CHECKSUM2);
        entity2.getLocation().setStorage(STORAGE2);
        entity2.getLocation().setUrl(URL2);
        doSave(entity2);
    }

    private void doSave(FileReference entity) {
        assumeThat(entity.getId()).isNull();
        final FileReference saved = repository.save(entity);
        assumeThat(saved).isNotNull();
        assumeThat(saved.getId()).isNotNull();
    }

    private FileReference newInstance() {

        final FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo();
        metaInfo.setFileName("test.pdf");
        metaInfo.setChecksum(CHECKSUM1);
        metaInfo.setAlgorithm("MD5");
        metaInfo.setMimeType(MimeType.valueOf(MediaType.APPLICATION_PDF_VALUE));

        final FileLocation location = new FileLocation();
        location.setStorage(STORAGE1);
        location.setUrl(URL1);

        final FileReference entity = new FileReference();
        entity.setLocation(location);
        entity.setMetaInfo(metaInfo);
        entity.setReferenced(true);
        entity.setStorageDate(OffsetDateTime.now());
        entity.getLazzyOwners().add(OWNER1);
        entity.getLazzyOwners().add(OWNER2);

        return entity;
    }

    @Test
    public void test3FindAll() {
        // GIVEN 2 entities
        // WHEN retrieving all entity
        final List<FileReference> all = repository.findAll();

        // THEN expect the saved entities to be retrieved
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getId()).isNotNull().isNotEqualTo(all.get(1).getId());
    }

    @Test
    public void test3FindByChecksums() {

        // WHEN retrieving the entity by checksums
        final Set<String> checksums = repository.findByMetaInfoChecksumIn(CHECKSUMS)
                                                .stream()
                                                .map(FileReference::getMetaInfo)
                                                .map(FileReferenceMetaInfo::getChecksum)
                                                .collect(Collectors.toSet());

        // THEN expect the checksum be
        assertThat(checksums).hasSize(2).containsExactlyInAnyOrderElementsOf(CHECKSUMS);
    }

    @Test
    public void test3FindByUrls() {

        // WHEN retrieving the entity by checksums
        final Set<String> urls = repository.findByLocationUrlIn(URLS)
                                           .stream()
                                           .map(FileReference::getLocation)
                                           .map(FileLocation::getUrl)
                                           .collect(Collectors.toSet());

        // THEN expect the urls be
        assertThat(urls).hasSize(2).containsExactlyInAnyOrderElementsOf(URLS);
    }

    @Test
    public void test3FindByStorageAndChecksum() {

        findByStorageAndChecksum(STORAGE1, CHECKSUM1, true);
        findByStorageAndChecksum(STORAGE2, CHECKSUM2, true);
        findByStorageAndChecksum(STORAGE1, CHECKSUM2, false);
        findByStorageAndChecksum(STORAGE2, CHECKSUM1, false);
    }

    private void findByStorageAndChecksum(String storage, String checksum, boolean expectedToBeFound) {
        // WHEN retrieving the entity by storage and checksum
        final FileReference entity = repository.findByLocationStorageAndMetaInfoChecksum(storage, checksum)
                                               .orElse(null);
        final boolean found = entity != null;
        // THEM expect the entity to be found if expectedToBeFound
        assertThat(found).isEqualTo(expectedToBeFound);
    }

    @Test
    public void test4Delete() {
        // GIVEN ids
        final Set<Long> ids = repository.findAll()
                                        .stream()
                                        .map(FileReference::getId)
                                        .collect(Collectors.toUnmodifiableSet());
        assumeThat(ids).hasSize(2);

        // WHEN delete
        ids.forEach(repository::deleteById);

        // THEN entity does no more exists
        ids.forEach(id -> assertThat(repository.findById(id)).isEmpty());
    }

}
