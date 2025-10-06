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
package fr.cnes.regards.modules.storage.client;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.fileaccess.dto.FileLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.request.FileGroupRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Test for method StorageClient#reference
 *
 * @author sbinda
 * @author onavarro
 */

public class StorageClientReferenceIT extends AbstractStorageClientIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientReferenceIT.class);

    @Test
    public void eventListenerTest() throws InterruptedException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        listener.reset();
        // Simulate multiples message from storage service
        int nbMessages = 1_000;
        for (int i = 0; i < nbMessages; i++) {
            final String groupId = "group_" + i;
            final String checksum = UUID.randomUUID().toString();
            FileReferenceMetaInfoDto metaInfo = new FileReferenceMetaInfoDto(checksum,
                                                                             "UUID",
                                                                             "file" + i,
                                                                             10L,
                                                                             null,
                                                                             null,
                                                                             MediaType.APPLICATION_JSON_VALUE,
                                                                             null);

            final FileLocationDto fileLocationDto = new FileLocationDto("storage", "path");
            final Set<String> owners = Set.of("owner");

            final FileReferenceDto reference = new FileReferenceDto(OffsetDateTime.now(),
                                                                    metaInfo,
                                                                    fileLocationDto,
                                                                    owners);

            final RequestResultInfoDto resultInfo = RequestResultInfoDto.build(groupId,
                                                                               checksum,
                                                                               fileLocationDto.getStorage(),
                                                                               fileLocationDto.getUrl(),
                                                                               owners,
                                                                               reference,
                                                                               null);

            publisher.publish(FileRequestsGroupEvent.build(groupId,
                                                           FileRequestType.STORAGE,
                                                           FileGroupRequestStatus.SUCCESS,
                                                           Set.of(resultInfo)));
        }
        LOGGER.info(" -------> Start waiting for all responses received !!!!!!!!!");
        // Wait for all events received
        waitRequestEnds(nbMessages);
    }

    @Test
    public void referenceWithSingleGroup() {
        super.referenceMultipleFiles();
    }

    @Test
    public void referenceWithMultipleGroups() {
        // GIVEN a set of ReferenceRequest such that it exceeds the number of MAX_REQUEST_PER_GROUP
        final Set<FileReferenceRequestDto> files = Sets.newHashSet();
        for (int i = 0; i < (FilesReferenceEvent.MAX_REQUEST_PER_GROUP + 1); i++) {
            files.add(FileReferenceRequestDto.build("file1.test",
                                                    UUID.randomUUID().toString(),
                                                    "UUID",
                                                    MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                                    10L,
                                                    "owner",
                                                    "somewhere",
                                                    "file://here/file1.test",
                                                    "source1",
                                                    "session1"));
        }

        // WHEN request the files to be referenced
        final Collection<RequestInfo> infos = client.reference(files);

        // THEN expect 2 groups of granted reference request to be created
        Assert.assertEquals("There should be two requests groups", 2, infos.size());
        waitRequestEnds(2);
        for (RequestInfo info : infos) {
            Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        }
    }

}
