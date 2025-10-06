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
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesRestorationRequestEvent;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Test for method StorageClient#makeAvailable
 *
 * @author sbinda
 * @author onavarro
 */

public class StorageClientAvailabilityIT extends AbstractStorageClientIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientAvailabilityIT.class);

    @Test
    public void availability() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Collection<RequestInfo> infos = client.makeAvailable(restorableFileChecksums, 24);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();

        waitRequestEnds(1);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

    }

    @Test
    public void availabilityWithUpdateOnAvailable() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();

        // File to retrieve should exists with default checksum
        Assert.assertTrue("File to retrieve should exists with default checksum",
                          fileRefService.search(NEARLINE_CONF,
                                                AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM)
                                        .isPresent());

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Collection<RequestInfo> infos = client.makeAvailable(Sets.newHashSet(AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM),
                                                             24);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();

        waitRequestEnds(1);

        Assert.assertFalse("File to retrieve should not exists anymore with default checksum",
                           fileRefService.search(NEARLINE_CONF,
                                                 AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM)
                                         .isPresent());
        Assert.assertTrue("File to retrieve should exists with updated checksum",
                          fileRefService.search(NEARLINE_CONF,
                                                AvailabilityUpdateCustomTestAction.getUpdatedChecksum(
                                                    AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM))
                                        .isPresent());

        // Check that fileRef checksum is updated
        fileRefService.search(NEARLINE_CONF, AvailabilityUpdateCustomTestAction.FILE_TO_UPDATE_CHECKSUM);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

    }

    @Test
    public void availabilityWithMultipleRequests() throws InterruptedException {
        Set<String> files = Sets.newHashSet();
        for (int i = 0; i < (FilesRestorationRequestEvent.MAX_REQUEST_PER_GROUP + 1); i++) {
            files.add(UUID.randomUUID().toString());
        }

        Collection<RequestInfo> infos = client.makeAvailable(files, 24);
        Assert.assertEquals("There should be two requests groups", 2, infos.size());
        waitRequestEnds(2);
        for (RequestInfo info : infos) {
            Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        }
    }

    @Test
    public void availability_offlineFiles() throws MalformedURLException, InterruptedException {

        this.referenceMultipleFiles();
        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<String> checksums = Sets.newHashSet();
        checksums.addAll(referenceFileChecksums);
        Collection<RequestInfo> infos = client.makeAvailable(checksums, 24);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.iterator().next();

        waitRequestEnds(1);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertFalse("Request should not be successful", listener.getSuccess().containsKey(info));
        Assert.assertTrue("Request should be error", listener.getErrors().containsKey(info));
        Assert.assertEquals("Number of error invalid",
                            referenceFileChecksums.size(),
                            listener.getErrors().get(info).size());
        for (String checksum : checksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors()
                                      .get(info)
                                      .stream()
                                      .anyMatch(e -> e.getRequestChecksum().equals(checksum)));
        }

    }

    @Test
    public void availability_offlineFilesAndRestoError() throws MalformedURLException, InterruptedException {

        this.storeFile();
        listener.reset();

        this.referenceMultipleFiles();
        listener.reset();

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<String> checksums = Sets.newHashSet();
        checksums.addAll(referenceFileChecksums);
        checksums.addAll(storedFileChecksums);
        int nbSuccessExpected = restorableFileChecksums.size();
        int nbErrorExpected = unrestorableFileChecksums.size() + referenceFileChecksums.size();
        Collection<RequestInfo> infos = client.makeAvailable(checksums, 24);
        Assert.assertEquals(1, infos.size());
        RequestInfo info = infos.stream().findFirst().get();

        waitRequestEnds(1);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should contains successful requests ", listener.getSuccess().containsKey(info));
        Assert.assertEquals("Request should contains successful requests ",
                            nbSuccessExpected,
                            listener.getSuccess().get(info).size());
        Assert.assertTrue("Request should contains error requests", listener.getErrors().containsKey(info));
        Assert.assertEquals("Request should contains error requests",
                            nbErrorExpected,
                            listener.getErrors().get(info).size());

        for (String checksum : referenceFileChecksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors()
                                      .get(info)
                                      .stream()
                                      .anyMatch(e -> e.getRequestChecksum().equals(checksum)));
        }

        for (String checksum : unrestorableFileChecksums) {
            Assert.assertTrue("Missing error checksum",
                              listener.getErrors()
                                      .get(info)
                                      .stream()
                                      .anyMatch(e -> e.getRequestChecksum().equals(checksum)));
        }
    }

}
