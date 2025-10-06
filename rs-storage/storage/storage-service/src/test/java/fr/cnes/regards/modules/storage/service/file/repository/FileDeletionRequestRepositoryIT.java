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
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.storage.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs;
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

import java.util.Set;
import java.util.UUID;

import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs.newFileReferenceRequestArgs1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author Olivier Navarro
 **/
@ActiveProfiles({ "noscheduler", "test" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = fr.cnes.regards.modules.storage.service.file.repository.FileReferenceRequestRepositoryIT.ScanningConfiguration.class)
public class FileDeletionRequestRepositoryIT extends AbstractDaoTransactionalIT {

    @Autowired
    private IFileReferenceRepository referenceRepository;

    @Autowired
    private IFileDeletetionRequestRepository deletionRepository;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    public static class ScanningConfiguration {

    }

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Test
    public void createDeletionRequestOnFileReference() {
        // GIVEN the creation of a FileReference
        final FileReferenceRequestArgs args = newFileReferenceRequestArgs1(); //.withStorage(ONLINE_CONF_LABEL);

        final FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo();
        metaInfo.setChecksum(args.getChecksum());
        metaInfo.setMimeType(MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE));
        metaInfo.setAlgorithm("MD5");
        metaInfo.setFileName(args.getFileName());
        metaInfo.setType(DataType.RAWDATA.toString());

        final FileLocation fileLocation = new FileLocation();
        fileLocation.setStorage(args.getStorage());
        fileLocation.setUrl(args.getUrl());
        fileLocation.setPendingActionRemaining(false);

        final FileReference fileReference = new FileReference(Set.of(args.getOwner()), metaInfo, fileLocation);

        final FileReference savedReference = referenceRepository.save(fileReference);

        final FileReference foundFileReference = referenceRepository.findByLocationStorageAndMetaInfoChecksum(args.getStorage(),
                                                                                                              args.getChecksum())
                                                                    .orElse(null);
        // assume that the FileReference has been created and can be found
        assumeThat(savedReference).as("FileReference should have been created").isNotNull();
        assumeThat(foundFileReference).as("FileReference should have been found by storage and checksum").isNotNull();
        final Set<FileReference> references = referenceRepository.findByMetaInfoChecksum(args.getChecksum());
        assumeThat(references).as("FileReference should have been found by checksum").hasSize(1);

        // WHEN saving a new FileDeletionRequest targeting the FileReference
        final FileDeletionRequest deletionRequest = new FileDeletionRequest(foundFileReference,
                                                                            false,
                                                                            UUID.randomUUID().toString(),
                                                                            FileRequestStatus.PENDING,
                                                                            args.getSessionOwner(),
                                                                            args.getSession());
        final FileDeletionRequest savedDeletionRequest = deletionRepository.save(deletionRequest);

        // THEN expect the FileDeletionRequest to be created and found.
        assertThat(savedDeletionRequest).isNotNull();
        assertThat(savedDeletionRequest.getId()).isNotNull();

        final FileDeletionRequest foundFileDeletionReq = deletionRepository.findByFileReferenceId(foundFileReference.getId())
                                                                           .orElse(null);
        assertThat(foundFileDeletionReq).isNotNull();
        assertThat(foundFileDeletionReq.getId()).isNotNull().isEqualTo(savedDeletionRequest.getId());
    }

}
