/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service;

import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;

/**
 * @author sbinda
 *
 */
@ActiveProfiles("disableStorageTasks")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_file_ref",
        "regards.storage.cache.path=target/cache", "regards.storage.cache.minimum.time.to.live.hours=12" })
public class FileReferenceServiceTest extends AbstractMultitenantServiceTest {

    @Autowired
    private FileReferenceService service;

    @Test
    public void requestNewFileReference() {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "file.test", 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation("anywhere", "anywhere://in/this/directory/file.test");

        service.createFileReference(owners, fileMetaInfo, origin, origin);

        Optional<FileReference> oFileRef = service.searchFileReference(origin.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileReferenceRequest> oFileRefReq = service.searchFileReferenceRequest(origin.getStorage(),
                                                                                        fileMetaInfo.getChecksum());
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          !oFileRefReq.isPresent());
    }

}
