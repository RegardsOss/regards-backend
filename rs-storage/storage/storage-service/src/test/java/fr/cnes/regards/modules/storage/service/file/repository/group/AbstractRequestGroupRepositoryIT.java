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

package fr.cnes.regards.modules.storage.service.file.repository.group;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.RequestGroup;
import fr.cnes.regards.modules.storage.service.file.repository.FileReferenceRequestRepositoryIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.util.MimeType;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Purpose of the tests are to find terminated group; Groups can be composed either of storage or reference request.<br>
 * groups have to be created in the concrete child class.
 * <br/>
 * The used finder is retrieving terminated group is to be implemented in the concrete child class.
 * An empty group is considered terminated. A group composed only
 * of terminated request is also considered as terminated.
 *
 * @author Olivier Navarro
 **/
@ActiveProfiles({ "noscheduler", "test" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_groups_tests" },
                    locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = FileReferenceRequestRepositoryIT.ScanningConfiguration.class)
public abstract class AbstractRequestGroupRepositoryIT extends AbstractDaoTransactionalIT {

    @Autowired
    IRequestGroupRepository groupRepository;

    @Autowired
    IFileReferenceRequestRepository referenceRequestRepository;

    @Autowired
    IFileStorageRequestRepository storageRequestRepository;

    protected static final Set<String> GROUPS = Set.of("group1", "group2", "group3", "group4");

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    public static class ScanningConfiguration {

    }

    @After
    public void tearDownGroupsAndRequests() {
        referenceRequestRepository.deleteAll();
        storageRequestRepository.deleteAll();
        groupRepository.deleteAll();
    }

    @Before
    public void assumeNoRequest() {
        tearDownGroupsAndRequests();
        assumeThat(referenceRequestRepository.findAll()).isEmpty();
        assumeThat(storageRequestRepository.findAll()).isEmpty();
    }

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Test
    public void allGroupWithNonTerminatedRequestThenExpectNoneFound() {
        // GIVEN no group of request in a terminated status

        // create group of 2 reference request in a non terminated state
        newRequest(FileRequestStatus.TO_DO, "group1");
        newRequest(FileRequestStatus.TO_DO, "group1");

        // create group of 2 reference request only one in a terminated state
        newRequest(FileRequestStatus.TO_DO, "group2");
        newRequest(FileRequestStatus.SUCCESS, "group2");

        // create 2nd group of 2 reference request in a terminated state
        // create group of 2 reference request only one in a terminated state
        newRequest(FileRequestStatus.ERROR, "group3");
        newRequest(FileRequestStatus.TO_DO, "group3");

        // create group of 2 reference request only one in a terminated state
        newRequest(FileRequestStatus.SUCCESS, "group4");
        newRequest(FileRequestStatus.PENDING, "group4");

        // WHEN find groups of terminated request
        final Set<String> groupIds = findGroupIds();

        // THEN no group found
        assertThat(groupIds).isEmpty();
    }

    @Test
    public void oneEmptyGroupAndOtherGroupWithNonTerminatedRequestThenExpectOneFound() {
        // GIVEN no group of request in a terminated status

        // create 1st group of 2 reference request in a terminated state
        newRequest(FileRequestStatus.TO_DO, "group1");
        newRequest(FileRequestStatus.TO_DO, "group1");

        // create 2nd group of 2 reference request in a terminated state
        newRequest(FileRequestStatus.TO_DO, "group2");
        newRequest(FileRequestStatus.SUCCESS, "group2");

        // create 2nd group of 2 reference request in a terminated state
        newRequest(FileRequestStatus.TO_DO, "group3");
        newRequest(FileRequestStatus.ERROR, "group3");

        // WHEN find groups of terminated request
        final Set<String> groupIds = findGroupIds();

        // THEN group with no request found
        assertThat(groupIds).containsExactlyInAnyOrder("group4");
    }

    @Test
    public void allEmptyGroupThenExpectAllFound() {
        // GIVEN no requests but empty groups

        // WHEN find groups of terminated request
        final Set<String> groupIds = findGroupIds();
        // THEN expect all empty group considered as terminated
        assertThat(groupIds).containsExactlyInAnyOrderElementsOf(GROUPS);
    }

    @Test
    public void oneGroupWithNonTerminatedRequestExpectOtherTerminatedFound() {
        // GIVEN 3 groups of reference requests in a terminated state
        // and 1 group of reference requests in a non terminated state

        // create 1st group of 2 reference request in a terminated state
        newRequest(FileRequestStatus.ERROR, "group1");
        newRequest(FileRequestStatus.SUCCESS, "group1");

        // create 2nd group of 2 reference request in a terminated state
        newRequest(FileRequestStatus.ERROR, "group2");
        newRequest(FileRequestStatus.ERROR, "group2");

        // create 3rd group of 2 reference request in a terminated state
        newRequest(FileRequestStatus.SUCCESS, "group3");
        newRequest(FileRequestStatus.SUCCESS, "group3");

        // create 4th group of reference request not all in a terminated state
        newRequest(FileRequestStatus.TO_DO, "group4");
        newRequest(FileRequestStatus.SUCCESS, "group4");
        newRequest(FileRequestStatus.ERROR, "group4");

        // WHEN retrieve all the group id with terminated request.
        final Set<String> terminatedGroupIds = findGroupIds();

        // THEN
        assertThat(terminatedGroupIds).containsExactlyInAnyOrder("group1", "group2", "group3");
    }

    abstract Set<String> findGroupIds();

    protected RequestGroup newReferenceGroup(String groupId) {
        return newRequestGroup(groupId, FileRequestType.REFERENCE);
    }

    protected RequestGroup newStorageGroup(String groupId) {
        return newRequestGroup(groupId, FileRequestType.STORAGE);
    }

    protected RequestGroup newRequestGroup(String groupId, FileRequestType type) {
        final RequestGroup group = new RequestGroup();
        group.setId(groupId);
        group.setType(type);
        group.setCreationDate(OffsetDateTime.now());
        group.setExpirationDate(OffsetDateTime.now().plusMinutes(2));
        return group;
    }

    protected void newRequest(FileRequestStatus status, String groupId) {

        final RequestGroup group = groupRepository.findById(groupId).orElse(null);
        switch (group.getType()) {
            case REFERENCE -> newReferenceRequest(status, groupId);
            case STORAGE -> newStorageRequest(status, groupId);
            default -> {
            }
        }
    }

    protected FileStorageRequestAggregation newStorageRequest(FileRequestStatus status, String groupId) {

        final FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo();
        metaInfo.setFileName("test.pdf");
        metaInfo.setChecksum(RandomChecksumUtils.generateRandomChecksum());
        metaInfo.setAlgorithm("MD5");
        metaInfo.setMimeType(MimeType.valueOf(MediaType.APPLICATION_PDF_VALUE));

        final FileStorageRequestAggregation entity = new FileStorageRequestAggregation();
        entity.setStorage("STORAGE");
        entity.setSession("SESSION");
        entity.setSessionOwner("SESSION OWNER");
        entity.setMetaInfo(metaInfo);

        entity.setStatus(status == FileRequestStatus.SUCCESS ? FileRequestStatus.ERROR : status);
        entity.getGroupIds().add(groupId);

        storageRequestRepository.save(entity);

        assumeThat(entity.getId()).isNotNull();
        return entity;
    }

    protected FileReferenceRequestAggregation newReferenceRequest(FileRequestStatus status, String groupId) {
        final FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo();
        metaInfo.setFileName("test.pdf");
        metaInfo.setChecksum(RandomChecksumUtils.generateRandomChecksum());
        metaInfo.setAlgorithm("MD5");
        metaInfo.setMimeType(MimeType.valueOf(MediaType.APPLICATION_PDF_VALUE));

        final FileReferenceRequestAggregation entity = new FileReferenceRequestAggregation();
        entity.setStorage("STORAGE");
        entity.setSession("SESSION");
        entity.setSessionOwner("SESSION OWNER");
        entity.setMetaInfo(metaInfo);

        entity.setStatus(status);
        entity.getGroupIds().add(groupId);

        referenceRequestRepository.save(entity);
        assumeThat(entity.getId()).isNotNull();
        return entity;
    }
}
