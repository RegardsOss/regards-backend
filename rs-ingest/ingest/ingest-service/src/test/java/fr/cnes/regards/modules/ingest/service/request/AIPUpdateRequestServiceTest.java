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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateCategoryTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateFileLocationTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTaskType;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * Test class for {@link AIPUpdateRequestService}
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_aip_update_request",
        "spring.jpa.show-sql=true" }, locations = { "classpath:application-test.properties" })
public class AIPUpdateRequestServiceTest extends AbstractIngestRequestTest {

    @Autowired
    private AIPUpdateRequestService aipUpdateReqService;

    @Autowired
    protected IAIPUpdateRequestRepository repo;

    @Test
    public void createNewUpdateRequestWithMultipleTasks() {
        String checksum = "checksum";
        String providerId = "providerId";
        initSipAndAip(checksum, providerId);
        Set<AbstractAIPUpdateTask> updateTasks = Sets.newHashSet();
        AIPUpdateFileLocationTask task = new AIPUpdateFileLocationTask();
        task.setType(AIPUpdateTaskType.ADD_FILE_LOCATION);
        task.setState(AIPUpdateState.READY);
        task.setFileLocationUpdates(Lists.newArrayList(RequestResultInfoDTO
                .build("groupId", "checksum", "somewhere", null, Sets.newHashSet("someone"),
                       simulatefileReference(checksum, aipEntity.getAipId()), null)));
        updateTasks.add(task);
        AIPUpdateCategoryTask catTask = new AIPUpdateCategoryTask();
        catTask.setType(AIPUpdateTaskType.ADD_CATEGORY);
        catTask.setState(AIPUpdateState.READY);
        catTask.setCategories(Lists.newArrayList("cat1", "cat2"));
        updateTasks.add(catTask);

        Assert.assertEquals(0, aipUpdateReqService.search(InternalRequestState.CREATED, PageRequest.of(0, 10))
                .getTotalElements());
        aipUpdateReqService.create(Sets.newHashSet(aipEntity), updateTasks);

        // Two new update requests should be created
        Assert.assertEquals(2, aipUpdateReqService.search(InternalRequestState.CREATED, PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    public void createUpdateRequestsOnAIPAlreadyUpdating() {
        String checksum = "checksum";
        String providerId = "providerId";
        initSipAndAip(checksum, providerId);
        Set<AbstractAIPUpdateTask> updateTasks = Sets.newHashSet();
        AIPUpdateFileLocationTask task = AIPUpdateFileLocationTask
                .buildAddLocationTask(Lists.newArrayList(RequestResultInfoDTO
                        .build("groupId", "checksum", "somewhere", null, Sets.newHashSet("someone"),
                               simulatefileReference(checksum, aipEntity.getAipId()), null)));
        AIPUpdateCategoryTask catTask = new AIPUpdateCategoryTask();
        catTask.setType(AIPUpdateTaskType.ADD_CATEGORY);
        catTask.setState(AIPUpdateState.READY);
        catTask.setCategories(Lists.newArrayList("cat1", "cat2"));

        updateTasks.add(task);
        updateTasks.add(catTask);

        Assert.assertEquals(0, aipUpdateReqService.search(InternalRequestState.CREATED, PageRequest.of(0, 10))
                .getTotalElements());
        aipUpdateReqService.create(Lists.newArrayList(aipEntity), updateTasks);
        // Two new update requests should be created
        Assert.assertEquals(2, aipUpdateReqService.search(InternalRequestState.CREATED, PageRequest.of(0, 10))
                .getTotalElements());

        // Simulate all requests running
        aipUpdateReqService.updateState(repo.findAll(), InternalRequestState.RUNNING);

        // Send the same requests
        AIPUpdateFileLocationTask newTask = AIPUpdateFileLocationTask
                .buildAddLocationTask(Lists.newArrayList(RequestResultInfoDTO
                        .build("groupId", "checksum", "somewhere", null, Sets.newHashSet("someone"),
                               simulatefileReference(checksum, aipEntity.getAipId()), null)));
        aipUpdateReqService.create(Lists.newArrayList(aipEntity), Sets.newHashSet(newTask));
        // The new update request should be blocked as requests are already running for the give aip
        Assert.assertEquals(1, aipUpdateReqService.search(InternalRequestState.BLOCKED, PageRequest.of(0, 10))
                .getTotalElements());
    }

}
