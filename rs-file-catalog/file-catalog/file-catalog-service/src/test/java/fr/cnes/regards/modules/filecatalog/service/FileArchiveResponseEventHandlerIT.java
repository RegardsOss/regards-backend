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
package fr.cnes.regards.modules.filecatalog.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileArchiveStatus;
import fr.cnes.regards.modules.filecatalog.amqp.input.FileArchiveResponseEvent;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.FileReferenceMetaInfo;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test for {@link fr.cnes.regards.modules.filecatalog.service.handler.FileArchiveResponseEventHandler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_archive_response_test" },
                    locations = { "classpath:application-test.properties" })
public class FileArchiveResponseEventHandlerIT extends AbstractFileCatalogIT {

    @Before
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void test_one_response() {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        Long requestId = saveNewRequest(checksum, "01");

        String responseFileUrl = "s3://newurl.com/file01";
        FileArchiveResponseEvent event = new FileArchiveResponseEvent(requestId, "storage", checksum, responseFileUrl);
        fileArchiveResponseEventHandler.handleBatch(List.of(event));

        Set<FileReference> fileReferences = fileReferenceRepository.findByMetaInfoChecksum(checksum);
        Assertions.assertEquals(1, fileReferences.size(), "There should be only one FileReference");

        FileReference fileReference = fileReferences.iterator().next();
        Assertions.assertEquals(FileArchiveStatus.TO_STORE,
                                fileReference.getLocation().getFileArchiveStatus(),
                                "The FileReference should be in TO_STORE status");
        Assertions.assertEquals(responseFileUrl,
                                fileReference.getLocation().getUrl(),
                                "The FileReference URL should be the once received in the event");
    }

    @Test
    public void test_multiple_responses() {
        int numberOfMessages = 5;
        String responseFileUrl = "s3://newurl.com/file0";
        String storage = "storage";
        List<String> checksums = IntStream.range(0, numberOfMessages)
                                          .mapToObj(i -> RandomChecksumUtils.generateRandomChecksum())
                                          .toList();
        List<FileArchiveResponseEvent> events = new ArrayList<>();
        for (int i = 0; i < numberOfMessages; i++) {
            FileArchiveResponseEvent event = new FileArchiveResponseEvent(saveNewRequest(checksums.get(i), "0" + i),
                                                                          storage,
                                                                          checksums.get(i),
                                                                          responseFileUrl + i);
            events.add(event);
        }

        fileArchiveResponseEventHandler.handleBatch(events);

        Pageable pageable = PageRequest.of(0, 100);
        List<FileReference> fileReferences = fileReferenceRepository.findByLocationStorage(storage, pageable)
                                                                    .getContent();
        Assertions.assertEquals(5, fileReferences.size(), "All message should have led to a FileReference creation");

        List<String> expectedUrls = IntStream.range(0, numberOfMessages)
                                             .mapToObj(i -> responseFileUrl + i)
                                             .collect(Collectors.toList());
        for (FileReference fileReference : fileReferences) {
            Assertions.assertEquals(FileArchiveStatus.TO_STORE,
                                    fileReference.getLocation().getFileArchiveStatus(),
                                    "The FileReference should be in TO_STORE status");
            Assertions.assertTrue(expectedUrls.contains(fileReference.getLocation().getUrl()),
                                  "The FileReference URL should be the once received in the event");
            expectedUrls.remove(fileReference.getLocation().getUrl());
        }

    }

    private Long saveNewRequest(String checksum, String id) {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(checksum,
                                                                   "MD5",
                                                                   "fileName" + id,
                                                                   1000L,
                                                                   MediaType.APPLICATION_OCTET_STREAM);

        FileStorageRequestAggregation request = new FileStorageRequestAggregation("owner",
                                                                                  metaInfo,
                                                                                  "http://url.com/" + id,
                                                                                  "storage",
                                                                                  Optional.of("sub/dir/"),
                                                                                  "groupId",
                                                                                  "sessionOwner",
                                                                                  "session",
                                                                                  false);

        request = fileStorageRequestAggregationRepository.save(request);
        return request.getId();
    }

}
