/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.request;

import java.util.Collection;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.service.flow.StorageResponseFlowHandler;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * Test class for {@link StorageResponseFlowHandler}
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_aip_update_request" },
        locations = { "classpath:application-test.properties" })
public class IngestStorageListenerTest extends AbstractIngestRequestTest {

    @Autowired
    private StorageResponseFlowHandler storageListener;

    @Autowired
    private AIPUpdateRequestService aipUpdateReqService;

    @Test
    public void testCopySuccessForUnknownFiles() {
        Set<RequestInfo> requests = Sets.newHashSet();
        Collection<RequestResultInfoDTO> successRequests = Sets.newHashSet();
        successRequests
                .add(RequestResultInfoDTO.build("groupId", "checksum", "somewhere", null, Sets.newHashSet("someone"),
                                                simulatefileReference("checksum", "someone"), null));
        requests.add(RequestInfo.build("groupId", successRequests, Sets.newHashSet()));
        Assert.assertEquals("At initialization no requests should be created", 0, aipUpdateReqService
                .search(InternalRequestState.CREATED, PageRequest.of(0, 10)).getTotalElements());
        storageListener.onCopySuccess(requests);
        Assert.assertEquals("No requests should be created", 0, aipUpdateReqService
                .search(InternalRequestState.CREATED, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    public void testCopySuccessForKnownFiles() {
        String checksum = "checksum";
        String providerId = "providerId";
        initSipAndAip(checksum, providerId);
        Set<RequestInfo> requests = Sets.newHashSet();
        Collection<RequestResultInfoDTO> successRequests = Sets.newHashSet();
        successRequests.add(RequestResultInfoDTO.build("groupId", checksum, "somewhere", null,
                                                       Sets.newHashSet(aipEntity.getAipId()),
                                                       simulatefileReference(checksum, aipEntity.getAipId()), null));
        successRequests.add(RequestResultInfoDTO.build("groupId", "other-file-checksum", "somewhere", null,
                                                       Sets.newHashSet("someone"),
                                                       simulatefileReference(checksum, "someone"), null));
        requests.add(RequestInfo.build("groupId", successRequests, Sets.newHashSet()));
        Assert.assertEquals("At initialization no requests should be created", 0, aipUpdateReqService
                .search(InternalRequestState.CREATED, PageRequest.of(0, 10)).getTotalElements());
        storageListener.onCopySuccess(requests);
        Assert.assertEquals("One request should be created. Two success requests are sent from storage but only one is associated to a known AIP",
                            1, aipUpdateReqService.search(InternalRequestState.CREATED, PageRequest.of(0, 10))
                                    .getTotalElements());
    }

}
