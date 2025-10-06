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
import fr.cnes.regards.modules.fileaccess.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesDeletionEvent;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Test for method StorageClient#delete
 *
 * @author sbinda
 * @author onavarro
 */
public class StorageClientDeleteIT extends AbstractStorageClientIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientDeleteIT.class);

    @Test
    public void deleteFile() {

        // Store file
        String checksum = UUID.randomUUID().toString();
        String owner = "delete-test";
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        RequestInfo info = client.store(FileStorageRequestDto.build("ok.file.test",
                                                                    checksum,
                                                                    "UUID",
                                                                    MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                                                    owner,
                                                                    SESSION_OWNER,
                                                                    SESSION,
                                                                    newUrl(),
                                                                    ONLINE_CONF,
                                                                    null));
        waitRequestEnds(1);

        Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(info));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(info));

        listener.reset();

        // Delete it
        RequestInfo deleteInfo = client.delete(FileDeletionDto.build(checksum,
                                                                     ONLINE_CONF,
                                                                     owner,
                                                                     SESSION_OWNER,
                                                                     SESSION,
                                                                     false));

        waitRequestEnds(1);
        Assert.assertTrue("Request should be granted", listener.getGranted().contains(deleteInfo));
        Assert.assertTrue("Request should be successful", listener.getSuccess().containsKey(deleteInfo));
        Assert.assertFalse("Request should not be error", listener.getErrors().containsKey(deleteInfo));

    }

    @Test
    public void deleteWithMultipleGroups() {
        Set<FileDeletionDto> files = Sets.newHashSet();
        for (int i = 0; i < (FilesDeletionEvent.MAX_REQUEST_PER_GROUP + 1); i++) {
            files.add(FileDeletionDto.build(UUID.randomUUID().toString(),
                                            ONLINE_CONF,
                                            "owner",
                                            SESSION_OWNER,
                                            SESSION,
                                            false));
        }
        Collection<RequestInfo> infos = client.delete(files);
        Assert.assertEquals("There should be two requests groups", 2, infos.size());
        waitRequestEnds(2);
        for (RequestInfo info : infos) {
            Assert.assertTrue("Request should be granted", listener.getGranted().contains(info));
        }
    }

}
