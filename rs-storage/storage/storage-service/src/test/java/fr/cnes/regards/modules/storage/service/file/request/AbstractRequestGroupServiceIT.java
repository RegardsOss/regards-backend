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

package fr.cnes.regards.modules.storage.service.file.request;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.OWNER1;
import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.OWNER2;

/**
 * @author Olivier Navarro
 **/
public abstract class AbstractRequestGroupServiceIT extends AbstractStorageIT {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractRequestGroupServiceIT.class);

    protected static final String SESSION_OWNER_1 = "SOURCE 1";

    protected static final String SESSION_1 = "SESSION 1";

    protected static final Set<String> OWNERS1 = Set.of(OWNER1);

    protected static final Set<String> OWNERS2 = Set.of(OWNER2);

    @Autowired
    protected RequestsGroupService reqGrpService;

    @Autowired
    protected IRequestGroupRepository reqGrpRepository;

    @Autowired
    protected IGroupRequestInfoRepository reqInfoRepo;

    @Before
    public void initialize() throws ModuleException {
        super.init();
        reqGrpRepository.deleteAll();
    }

    @Test
    public void testPerfCheckGrp() {

        for (int i = 0; i < 2000; i++) {
            // Simulate a request ends success
            String groupId = UUID.randomUUID().toString();

            // Simulate a running request
            if (i < 1000) {
                saveNewRequest(groupId, FileRequestStatus.TO_DO, null);
            }
            saveNewRequest(groupId, FileRequestStatus.ERROR, "toto la belle erreur");

            // Grant a group requests
            reqGrpService.granted(groupId, FileRequestType.REFERENCE, 5, OffsetDateTime.now().plusDays(120));
            requestSuccess(groupId, FileRequestType.REFERENCE);
            requestSuccess(groupId, FileRequestType.REFERENCE);
            requestSuccess(groupId, FileRequestType.REFERENCE);
            requestSuccess(groupId, FileRequestType.REFERENCE);
            if (i >= 10) {
                requestSuccess(groupId, FileRequestType.REFERENCE);
            }
        }
        long start = System.currentTimeMillis();
        reqGrpService.checkRequestsGroupsDone();
        LOGGER.info("DONE in {} ms", System.currentTimeMillis() - start);
    }

    abstract protected void saveNewRequest(String groupId,
                                           FileRequestStatus fileRequestStatus,
                                           String totoLaBelleErreur);

    protected void requestSuccess(String groupId, FileRequestType requestType) {
        reqGrpService.requestSuccess(groupId,
                                     requestType,
                                     RandomChecksumUtils.generateRandomChecksum(),
                                     ONLINE_CONF_LABEL,
                                     null,
                                     OWNERS1,
                                     null);
    }

    protected FileReferenceMetaInfo newFileReferenceMetaInfo() {
        return new FileReferenceMetaInfo(RandomChecksumUtils.generateRandomChecksum(),
                                         "MD5",
                                         "plop",
                                         10L,
                                         MediaType.APPLICATION_ATOM_XML);
    }

    @Test
    public void checkGroupDone() {
        for (FileRequestType type : FileRequestType.values()) {
            String groupId = UUID.randomUUID().toString();
            // Grant a group requests
            reqGrpService.granted(groupId, type, 2, OffsetDateTime.now().plusSeconds(120));
            // Simulate a request ends success
            requestSuccess(groupId, type);
            // Simulate a requests ends error
            reqGrpService.requestError(groupId,
                                       type,
                                       RandomChecksumUtils.generateRandomChecksum(),
                                       ONLINE_CONF_LABEL,
                                       null,
                                       OWNERS1,
                                       null);
            // Group should be created
            Assert.assertTrue("Error during group request creation", reqGrpRepository.findById(groupId).isPresent());
            Assert.assertEquals("There be requests infos for expired group", 2, reqInfoRepo.count());
            // Check group is terminated
            reqGrpService.checkRequestsGroupsDone();
            // Group should not exists anymore
            Assert.assertFalse("Request group should be deleted as no requests are associated",
                               reqGrpRepository.findById(groupId).isPresent());
            // No request info should remains
            Assert.assertTrue("There should be no remaining request infos in success",
                              reqInfoRepo.findByGroupIdAndError(groupId, false).isEmpty());
            Assert.assertTrue("There should be no remaining request infos in error",
                              reqInfoRepo.findByGroupIdAndError(groupId, true).isEmpty());

        }
    }

}
