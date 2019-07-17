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
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "disableStorageTasks" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_deletion_tests",
        "regards.storage.cache.path=target/cache", "regards.storage.cache.minimum.time.to.live.hours=12" })
public class FileDeletionTest extends AbstractFileReferenceTest {

    @Before
    public void initialize() throws ModuleException {
        super.init();
    }

    @Test
    public void deleteFileReference() throws EntityNotFoundException, InterruptedException, ExecutionException {
        String storage = "anywhere";
        List<String> owners = Lists.newArrayList("someone", "someone-else");
        Optional<FileReference> oFileRef = referenceRandomFile(owners, Lists.newArrayList(), "file1.test", storage);
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Optional<FileReferenceRequest> oFileRefReq = fileRefRequestService
                .search(oFileRef.get().getLocation().getStorage(), oFileRef.get().getMetaInfo().getChecksum());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          !oFileRefReq.isPresent());
        FileReference fileRef = oFileRef.get();

        fileRefService.removeOwner(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(),
                                   owners.get(0));

        Optional<FileReference> afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(),
                                                                      fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should be always existing", afterDeletion.isPresent());
        Assert.assertEquals("File reference should always be owned by one owner", 1,
                            afterDeletion.get().getOwners().size());
        Assert.assertTrue("File reference should always be owned by one owner",
                          afterDeletion.get().getOwners().contains(owners.get(1)));

        fileRefService.removeOwner(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(),
                                   owners.get(1));

        afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        Assert.assertFalse("File reference should not existing anymore", afterDeletion.isPresent());

    }

}
