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

import fr.cnes.regards.modules.fileaccess.dto.request.FileCopyDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for method StorageClient#copy
 *
 * @author sbinda
 * @author onavarro
 */
public class StorageClientCopyIT extends AbstractStorageClientIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientCopyIT.class);

    @Test
    public void copy() {

        this.storeFile();
        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<FileCopyDto> requests = restorableFileChecksums.stream()
                                                           .map(f -> FileCopyDto.build(f,
                                                                                       NEARLINE_CONF_2,
                                                                                       SESSION_OWNER,
                                                                                       SESSION))
                                                           .collect(Collectors.toSet());
        Collection<RequestInfo> infos = client.copy(requests);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.iterator().next();
        // 1 Copy group requests should be over
        // 1 Availability requests should be over (created by the copy process)
        // 1 Storage group by file. Each group is created after availability event for each file.
        waitRequestEnds(1 + 1 + restorableFileChecksums.size(), 30);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue(String.format("Request should be successful for request id %s", info.getGroupId()),
                          listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));
    }

    @Test
    public void copyWithAvailableUpdate() {
        this.storeFile();
        listener.reset();

        // File to retrieve should exists with default checksum
        Assert.assertTrue("File to retrieve should exists with default checksum",
                          fileRefService.search(NEARLINE_CONF,
                                                AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM)
                                        .isPresent());

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        Set<FileCopyDto> requests = Set.of(FileCopyDto.build(AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM,
                                                             ONLINE_CONF,
                                                             SESSION_OWNER,
                                                             SESSION));
        Collection<RequestInfo> infos = client.copy(requests);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.iterator().next();
        LOGGER.info("[TEST COPY] Running copy group request {} with {} requests", info.getGroupId(), requests.size());
        // 1 Copy request
        // 1 Availability request
        // 1 Storage request
        waitRequestEnds(3, 40);

        Assert.assertTrue("Request group should be granted", listener.getGranted().contains(info));
    }

    @Test
    public void copy_withError() {

        this.storeFile();
        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<FileCopyDto> requests = storedFileChecksums.stream()
                                                       .map(f -> FileCopyDto.build(f,
                                                                                   NEARLINE_CONF_2,
                                                                                   SESSION_OWNER,
                                                                                   SESSION))
                                                       .collect(Collectors.toSet());
        Collection<RequestInfo> infos = client.copy(requests);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.iterator().next();
        LOGGER.info("[TEST COPY] Running copy group request {} with {} requests", info.getGroupId(), requests.size());
        // 1 Copy request
        // 1 Availability request
        // X Storage request
        waitRequestEnds(1 + 1 + storedFileChecksums.size(), 60);

        Assert.assertTrue("Request group should be granted", listener.getGranted().contains(info));

        // asserting success file
        Assert.assertTrue(String.format("Request group %s should contains 3 successful request", info.getGroupId()),
                          listener.getSuccess().containsKey(info));
        Assert.assertEquals(String.format("Request group %s should contains 3 successful request", info.getGroupId()),
                            restorableFileChecksums.size(),
                            listener.getSuccess().get(info).size());

        final Set<String> actualChecksumsOfFileInSuccess = listener.getSuccess()
                                                                   .get(info)
                                                                   .stream()
                                                                   .map(RequestResultInfoDto::getRequestChecksum)
                                                                   .collect(Collectors.toUnmodifiableSet());
        assertThat(actualChecksumsOfFileInSuccess).as("Missing an success file")
                                                  .hasSize(3)
                                                  .as("Missing an success file")
                                                  .containsExactlyInAnyOrderElementsOf(restorableFileChecksums);
        // asserting error file
        Assert.assertTrue("Request group should be in error", listener.getErrors().containsKey(info));
        Assert.assertEquals(String.format("Request group %s should contains 1 error request", info.getGroupId()),
                            unrestorableFileChecksums.size(),
                            listener.getErrors().get(info).size());
        final Set<String> actualChecksumsOfFileInErrors = listener.getErrors()
                                                                  .get(info)
                                                                  .stream()
                                                                  .map(RequestResultInfoDto::getRequestChecksum)
                                                                  .collect(Collectors.toUnmodifiableSet());
        assertThat(actualChecksumsOfFileInErrors).as("Missing an error file")
                                                 .hasSize(1)
                                                 .as("Missing an error file")
                                                 .containsExactlyInAnyOrderElementsOf(unrestorableFileChecksums);

    }

}
