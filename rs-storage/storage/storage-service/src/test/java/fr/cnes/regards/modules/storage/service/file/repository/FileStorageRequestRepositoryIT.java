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
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Test some of the basic CRUD methods of the repository: {@link IFileStorageRequestRepository}.
 *
 * @author Olivier Navarro
 **/
@ActiveProfiles({ "noscheduler", "test" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = FileStorageRequestRepositoryIT.ScanningConfiguration.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileStorageRequestRepositoryIT extends AbstractDaoTransactionalIT {

    @Autowired
    IFileStorageRequestRepository repository;

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
        final FileStorageRequestAggregation entity = newInstance();
        assumeThat(entity.getId()).isNull();
        repository.save(entity);
    }

    @Test
    public void test1Create() {
        // GIVEN a new entity
        final FileStorageRequestAggregation entity = newInstance();
        assumeThat(entity.getId()).isNull();
        // WHEN saving it
        final FileStorageRequestAggregation saved = repository.save(entity);
        // THEN expect id to be non null
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
    }

    private FileStorageRequestAggregation newInstance() {
        final FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo();
        metaInfo.setFileName("test.pdf");
        metaInfo.setChecksum("01234567890123456789abcdefABCDEF");
        metaInfo.setAlgorithm("MD5");
        metaInfo.setMimeType(MimeType.valueOf(MediaType.APPLICATION_PDF_VALUE));
        final FileStorageRequestAggregation entity = new FileStorageRequestAggregation();
        entity.setStatus(FileRequestStatus.TO_DO);
        entity.setStorage("STORAGE");
        entity.setSession("SESSION");
        entity.setSessionOwner("SESSION OWNER");
        entity.setMetaInfo(metaInfo);
        return entity;
    }

    @Test
    public void test2FindAll() {
        // GIVEN the id of the single saved entity
        final Long id = getId();

        // WHEN retrieving all the entity
        final List<FileStorageRequestAggregation> all = repository.findAll();

        // THEN expect the single saved entity to be retrieved
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getId()).isNotNull().isEqualTo(id);
    }

    @Test
    public void test2FindById() {
        // GIVEN the id of the single saved entity
        final Long id = getId();

        // WHEN retrieving the entity by id
        Optional<FileStorageRequestAggregation> found = repository.findById(id);

        // THEN expect the saved entity to be retrieved has the same id.
        assertThat(found).isPresent().map(FileStorageRequestAggregation::getId).get().isNotNull().isEqualTo(id);
    }

    @Test
    public void test3UpdateStatusAndJobId() {
        // GIVEN the id of the single saved entity
        final Long id = getId();
        assumeThat(id).isNotNull();

        // WHEN updating the jobid and status of the entity from its id
        int updatedCount = repository.updateStatusAndJobId(FileRequestStatus.PENDING, "jobid 1", id);
        // update is done againt the database and leaves the persistence context outdated.
        entityManager.clear();

        // THEN expect the update to be successful
        assertThat(updatedCount).isEqualTo(1);

        // and the entity updated
        final FileStorageRequestAggregation updated = getEntity(id);
        assertThat(updated.getId()).as("id is not null").isEqualTo(id);
        assertThat(updated.getJobId()).as("JobId is not null").isNotNull();
        assertThat(updated.getStatus()).as("status is now PENDING").isEqualTo(FileRequestStatus.PENDING);
    }

    @Test
    public void test3UpdateStatusAndJobIdTheBadWay() {
        // GIVEN the id of the single saved entity
        final Long id = getId();
        assumeThat(id).isNotNull();

        // WHEN updating the jobid and status of the entity from its id
        int updatedCount = repository.updateStatusAndJobId(FileRequestStatus.PENDING, "jobid 1", id);
        // update is done againt the database and leaves the persistence context outdated.

        // THEN expect the update to be successful
        assertThat(updatedCount).isEqualTo(1);

        // but the entity is outdated in the persistence context
        final FileStorageRequestAggregation updated = getEntity(id);
        assertThat(updated.getId()).as("id is unchanged").isEqualTo(id);
        assertThat(updated.getJobId()).as("outdated JobId is still null").isNull();
        assertThat(updated.getStatus()).as("outdated status is still in a TO_DO").isEqualTo(FileRequestStatus.TO_DO);
    }

    @Test
    public void test3UpdateStatusAndError() {
        // GIVEN the id of the single saved entity
        final Long id = getId();
        assumeThat(id).isNotNull();

        // WHEN updating the errorcause and status of the entity
        int updatedCount = repository.updateError(FileRequestStatus.ERROR, "referencing unknown failure", id);
        entityManager.clear();

        // THEN expect the update to be successful
        assertThat(updatedCount).isEqualTo(1);

        final FileStorageRequestAggregation updated = getEntity(id);
        assertThat(updated.getId()).as("id is unchanged").isEqualTo(id);
        assertThat(updated.getErrorCause()).as("error cause is not null").isNotNull();
        assertThat(updated.getStatus()).as("status is not null").isNotNull();
    }

    @Test
    public void test3Update() {
        // GIVEN presence of a single entity
        final FileStorageRequestAggregation entity = getEntity();
        assumeThat(entity.getStatus()).isEqualTo(FileRequestStatus.TO_DO);

        // WHEN updating the jobid and status of the entity
        entity.setJobId("jobid 1");
        entity.setStatus(FileRequestStatus.PENDING);
        repository.save(entity);

        // THEN expect the update to be successful
        final FileStorageRequestAggregation updated = repository.findById(entity.getId()).orElseThrow();
        assertThat(updated.getId()).isEqualTo(entity.getId());
        assertThat(updated.getStatus()).isEqualTo(FileRequestStatus.PENDING);
    }

    @Test
    public void test4Delete() {
        // GIVEN a new saved entity
        final Long id = getId();
        // WHEN delete
        repository.deleteById(id);
        // THEN entity does no more exists
        assertThat(repository.findById(id)).isEmpty();
    }

    private Long getId() {
        return repository.findAll().stream().findFirst().map(FileStorageRequestAggregation::getId).orElseThrow();
    }

    private FileStorageRequestAggregation getEntity() {
        return repository.findAll().stream().findFirst().orElseThrow();
    }

    private FileStorageRequestAggregation getEntity(long id) {
        return repository.findById(id).orElseThrow();
    }
}
