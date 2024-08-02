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
package fr.cnes.regards.modules.ingest.service.session;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISessionDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISourceDeleteService;
import fr.cnes.regards.modules.ingest.dao.AIPSpecificationsBuilder;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test the deletion of a source or a session following a {@link SourceDeleteEvent} or a {@link SessionDeleteEvent}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_session_delete_it",
                                   "regards.amqp.enabled=true",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "testAmqp", "StorageClientMock" })
public class SessionDeleteIT extends IngestMultitenantServiceIT {

    @Autowired
    private IPublisher publisher;

    @Autowired
    private StorageClientMock storageClient;

    @SpyBean
    private ISourceDeleteService sourceDeleteService;

    @SpyBean
    private ISessionDeleteService sessionDeleteService;

    private static final String SOURCE_1 = "SOURCE 1";

    private static final String SOURCE_2 = "SOURCE 2";

    private static final String SESSION_1 = "SESSION 1";

    private static final String SESSION_2 = "SESSION 2";

    @Override
    public void doInit() {
        initData();
    }

    @Test
    public void testDeleteSource() {
        // publish source deletion event to delete SOURCE 1
        publisher.publish(new SourceDeleteEvent(SOURCE_1));
        Mockito.verify(sourceDeleteService, Mockito.timeout(1000L).times(1)).deleteSource(SOURCE_1);

        // wait for deletion of all aips linked to SOURCE 1
        long wait = FIVE_SECONDS * 10;
        ingestServiceTest.waitAllRequestsFinished(wait, getDefaultTenant());

        // test aips linked to SOURCE 1 are not present
        SearchAIPsParameters filters = new SearchAIPsParameters().withSessionOwner(SOURCE_1);
        Page<AIPEntity> aipsDeleted = aipRepository.findAll(new AIPSpecificationsBuilder().withParameters(filters)
                                                                                          .build(),
                                                            PageRequest.of(0, 10));
        Assert.assertEquals("AIPs should have been deleted", 0, aipsDeleted.getContent().size());

        // test aips linked to SOURCE 2 are present
        filters = new SearchAIPsParameters().withSessionOwner(SOURCE_2);
        Page<AIPEntity> aips = aipRepository.findAll(new AIPSpecificationsBuilder().withParameters(filters).build(),
                                                     PageRequest.of(0, 10));
        Assert.assertNotEquals("AIPs should have been present", 0, aips.getContent().size());
    }

    @Test
    public void testSessionDelete() {
        // publish session deletion event to delete SESSION 1 of SOURCE 1
        publisher.publish(new SessionDeleteEvent(SOURCE_1, SESSION_1));
        Mockito.verify(sessionDeleteService, Mockito.timeout(1000L).times(1)).deleteSession(SOURCE_1, SESSION_1);

        // wait for deletion of all aips linked to SESSION 1 of SOURCE 1
        long wait = FIVE_SECONDS * 10;
        ingestServiceTest.waitAllRequestsFinished(wait, getDefaultTenant());

        // test aips linked to SESSION 1 of SOURCE 1 are not present

        SearchAIPsParameters filters = new SearchAIPsParameters().withSessionOwner(SOURCE_1).withSession(SESSION_1);
        Page<AIPEntity> aipsDeleted = aipRepository.findAll(new AIPSpecificationsBuilder().withParameters(filters)
                                                                                          .build(),
                                                            PageRequest.of(0, 10));
        Assert.assertEquals(0, aipsDeleted.getContent().size());

        // test aips linked to SESSION 2 SOURCE 1 are present
        filters = new SearchAIPsParameters().withSessionOwner(SOURCE_1).withSession(SESSION_2);
        Page<AIPEntity> aips1 = aipRepository.findAll(new AIPSpecificationsBuilder().withParameters(filters).build(),
                                                      PageRequest.of(0, 10));
        Assert.assertNotEquals("AIPs should have been present", 0, aips1.getContent().size());

        // test aips linked to SESSION 1 SOURCE 2 are present
        filters = new SearchAIPsParameters().withSessionOwner(SOURCE_2).withSession(SESSION_1);
        Page<AIPEntity> aips2 = aipRepository.findAll(new AIPSpecificationsBuilder().withParameters(filters).build(),
                                                      PageRequest.of(0, 10));
        Assert.assertNotEquals("AIPs should have been present", 0, aips2.getContent().size());

    }

    private void initData() {
        long nbSIP = 3;
        storageClient.setBehavior(true, true);
        publishSIPEvent(create("1", Lists.newArrayList("TAG_0")),
                        "STORAGE_1",
                        SESSION_1,
                        SOURCE_1,
                        Lists.newArrayList("CATEGORIES_0"));
        publishSIPEvent(create("1", Lists.newArrayList("TAG_0")),
                        "STORAGE_1",
                        SESSION_2,
                        SOURCE_1,
                        Lists.newArrayList("CATEGORIES_0"));
        publishSIPEvent(create("2", Lists.newArrayList("TAG_0")),
                        "STORAGE_1",
                        SESSION_1,
                        SOURCE_2,
                        Lists.newArrayList("CATEGORIES_0"));
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED, getDefaultTenant());
        long wait = FIVE_SECONDS * 3;

        mockNotificationSuccess(RequestTypeConstant.INGEST_VALUE);
    }

}