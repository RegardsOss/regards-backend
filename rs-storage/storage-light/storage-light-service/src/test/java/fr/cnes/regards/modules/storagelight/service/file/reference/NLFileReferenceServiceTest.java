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
package fr.cnes.regards.modules.storagelight.service.file.reference;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.commons.compress.utils.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;

/**
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" })
public class NLFileReferenceServiceTest extends AbstractFileReferenceTest {

    @Before
    public void initialize() throws ModuleException {
        super.init();
    }

    @Test(expected = EntityNotFoundException.class)
    public void download_without_cache() throws InterruptedException, ExecutionException, EntityNotFoundException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        try {
            nearlineFileRefService.download(fileRef);
        } finally {
            Assert.assertTrue("A cache request should be done for the near line file to download",
                              fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());
        }
    }

    @Test
    public void download_with_cache()
            throws InterruptedException, ExecutionException, EntityNotFoundException, IOException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        this.simulateFileInCache(fileRef.getMetaInfo().getChecksum());
        InputStream stream = nearlineFileRefService.download(fileRef);
        Assert.assertNotNull(stream);
        stream.close();
    }

    @Test
    public void makeAvailable_without_cache() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        Mockito.clearInvocations(publisher);
        nearlineFileRefService.makeAvailable(Sets.newHashSet(fileRef), OffsetDateTime.now().plusDays(1),
                                             UUID.randomUUID().toString());
        Assert.assertTrue("A cache request should be done for the near line file to download",
                          fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());
        Mockito.verify(publisher, Mockito.never()).available(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void makeAvailable_with_cache() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        Mockito.clearInvocations(publisher);
        this.simulateFileInCache(fileRef.getMetaInfo().getChecksum());
        nearlineFileRefService.makeAvailable(Sets.newHashSet(fileRef), OffsetDateTime.now().plusDays(1),
                                             UUID.randomUUID().toString());
        Assert.assertFalse("No cache request should be created for the near line file to download as it is available in cache",
                           fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());
        Mockito.verify(publisher, Mockito.times(1)).available(Mockito.any(), Mockito.any(), Mockito.any());
    }

}
