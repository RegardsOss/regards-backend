/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.order.clean;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.service.order.clean.job.CleanDeliveryOrderJob;
import fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils;
import fr.cnes.regards.modules.delivery.service.order.zip.workspace.DeliveryDownloadWorkspaceManager;
import fr.cnes.regards.modules.order.amqp.input.OrderCancelRequestDtoEvent;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * Test for {@link CleanDeliveryOrderJob}.
 * <p>The purpose of this test is to verify that cleaning tasks are executed properly.</p>
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class CleanDeliveryOrderJobTest {

    @Spy
    private IPublisher publisher;

    @Mock
    private IWorkspaceService workspaceService;

    @Before
    public void init() throws IOException, DeliveryOrderException {
        // clean workspace directory if it already exists
        FileUtils.deleteDirectory(DeliveryStepUtils.WORKSPACE_PATH.toFile());
        Files.createDirectories(DeliveryStepUtils.WORKSPACE_PATH);
        Mockito.when(workspaceService.getMicroserviceWorkspace()).thenReturn(DeliveryStepUtils.WORKSPACE_PATH);
    }

    @Test
    public void givenCleanDeliveryOrderJob_whenExecuted_thenCleanTasksPerformed() throws DeliveryOrderException {
        // GIVEN
        CleanDeliveryOrderJob cleanDeliveryOrderJob = initCleanDeliveryOrderJob();
        // create delivery workspace path
        DeliveryDownloadWorkspaceManager downloadWorkspaceManager = new DeliveryDownloadWorkspaceManager(
            DeliveryStepUtils.DELIVERY_CORRELATION_ID,
            DeliveryStepUtils.WORKSPACE_PATH);
        downloadWorkspaceManager.createDeliveryFolder();

        // WHEN
        cleanDeliveryOrderJob.run();

        // THEN
        Assertions.assertThat(downloadWorkspaceManager.isDeliveryTmpFolderPathExists()).isFalse();
        ArgumentCaptor<List<OrderCancelRequestDtoEvent>> cancelEventCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher).publish(cancelEventCaptor.capture());
        Assertions.assertThat(cancelEventCaptor.getValue().get(0).getCorrelationId())
                  .isEqualTo(DeliveryStepUtils.DELIVERY_CORRELATION_ID);
    }

    private CleanDeliveryOrderJob initCleanDeliveryOrderJob() {
        // init clean service
        CleanOrderService cleanOrderService = new CleanOrderService(workspaceService, publisher);

        // init job
        CleanDeliveryOrderJob cleanDeliveryOrderJob = new CleanDeliveryOrderJob();
        ReflectionTestUtils.setField(cleanDeliveryOrderJob,
                                     "deliveryRequests",
                                     List.of(DeliveryStepUtils.buildDeliveryRequest()));
        ReflectionTestUtils.setField(cleanDeliveryOrderJob, "cleanOrderService", cleanOrderService);
        ReflectionTestUtils.setField(cleanDeliveryOrderJob, "jobInfoId", UUID.randomUUID());

        return cleanDeliveryOrderJob;
    }
}
