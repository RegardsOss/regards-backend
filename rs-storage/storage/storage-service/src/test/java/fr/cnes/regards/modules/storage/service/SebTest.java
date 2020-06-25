/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service;

import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;

/**
 *
 * @author Sébastien Binda
 *
 */
@ActiveProfiles({ "test", "default" })
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=seb_test",
                "regards.storage.cache.path=target/cache", "regards.storage.cache.size.limit.ko.per.tenant:10" },
        locations = { "classpath:application-local.properties" })
public class SebTest extends AbstractStorageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageTest.class);

    @Autowired
    private FileReferenceService fileRefService;

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();

        if (fileRefRepo.count() == 0) {
            // Insert many refs
            Set<FileReference> toSave = Sets.newHashSet();
            for (Long i = 0L; i < 100_000; i++) {
                FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "UUID",
                        "file_" + i + ".test", i, MediaType.APPLICATION_OCTET_STREAM);
                FileLocation location = new FileLocation("storage_" + i, "storage://plop/file");
                FileReference fileRef = new FileReference(Lists.newArrayList("owner"), metaInfo, location);
                toSave.add(fileRef);
                if (toSave.size() >= 10_000) {
                    long start = System.currentTimeMillis();
                    fileRefRepo.saveAll(toSave);
                    LOGGER.info("Saves {} done in {}ms", toSave.size(), System.currentTimeMillis() - start);
                    toSave.clear();
                }
            }

            // Init nearline stored refs
            long start = System.currentTimeMillis();
            fileRefRepo.saveAll(toSave);
            LOGGER.info("Saves {} NearLines done in {}ms", toSave.size(), System.currentTimeMillis() - start);
        }
    }

    @Test
    public void test() throws InterruptedException {
        fileRefService.keepTransaction();
    }

}
