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
package fr.cnes.regards.modules.delivery.service.order.zip.job;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.integration.test.job.AbstractMultitenantServiceWithJobIT;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.s3.client.S3HighLevelReactiveClient;
import fr.cnes.regards.framework.s3.domain.*;
import fr.cnes.regards.framework.s3.exception.S3ClientException;
import fr.cnes.regards.framework.s3.test.S3BucketTestUtils;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.dao.IDeliveryAndJobRepository;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import fr.cnes.regards.modules.delivery.domain.settings.S3DeliveryServer;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.config.OrderDeliveryTestConfiguration;
import fr.cnes.regards.modules.delivery.service.order.zip.env.config.TestDeliveryServerProperties;
import fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils.DELIVERY_CORRELATION_ID;
import static fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils.MULTIPLE_FILES_ZIP_NAME_PATTERN;

/**
 * Test for {@link OrderDeliveryZipJob}.
 * <p>The purpose of this test is to check if a zip can be successfully created and delivered to a S3 server after a
 * {@link DeliveryRequest}.</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenDeliveryRequest_whenZipJobRun_thenZipCreatedOnS3()}</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link #givenDeliveryRequest_whenZipJobRunInError_thenError()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles({ "testAmqp", "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=delivery_zip_job_it",
                                   "regards.amqp.enabled=true" })
@ContextConfiguration(classes = { OrderDeliveryTestConfiguration.class })
@SpringBootTest
public class OrderDeliveryZipJobIT extends AbstractMultitenantServiceWithJobIT {

    @Autowired
    protected DynamicTenantSettingService dynamicTenantSettingService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IDeliveryRequestRepository deliveryRequestRepository;

    @Autowired
    private IDeliveryAndJobRepository deliveryAndJobRepository;

    @Autowired
    private TestDeliveryServerProperties testS3;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private IJobInfoService jobInfoService;

    private S3Server s3Server;

    @Before
    public void init() throws Exception {
        clean();
        initS3Settings(testS3);
        s3Server = getS3Server();
        S3BucketTestUtils.createBucket(s3Server);
    }

    @After
    public void reset() {
        S3BucketTestUtils.deleteBucket(s3Server);
    }

    @Test
    public void givenDeliveryRequest_whenZipJobRun_thenZipCreatedOnS3() throws URISyntaxException, IOException {
        // GIVEN
        DeliveryAndJob deliveryAndJob = initDeliveryRequestAndJob();
        JobInfo jobInfo = deliveryAndJob.getJobInfo();

        // WHEN
        this.getJobTestUtils().runAndWaitJob(jobInfo, 122225);

        // THEN
        // check job OrderDeliveryZipJob is in success
        Assertions.assertThat(jobInfoService.retrieveJob(jobInfo.getId()).getStatus().getStatus())
                  .isEqualTo(JobStatus.SUCCEEDED);
        // check zip was created
        checkZipDeliveryExistsOnS3();
        // check workspace was deleted
        Assertions.assertThat(workspaceService.getMicroserviceWorkspace().toFile()).isEmptyDirectory();
        // check request was deleted
        Assertions.assertThat(deliveryRequestRepository.findAll()).isEmpty();
        // check success message was sent
        ArgumentCaptor<ISubscribable> captorPublished = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(3)).publish(captorPublished.capture());
        List<ISubscribable> eventsSent = captorPublished.getAllValues();
        checkDeliveryResponseSent(eventsSent, DeliveryRequestStatus.DONE);
        checkJobEventSent(eventsSent, JobEventType.SUCCEEDED);
    }

    @Test
    public void givenDeliveryRequest_whenZipJobRunInError_thenError()
        throws IOException, EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        // GIVEN
        // update delivery bucket conf to make s3 unreachable
        dynamicTenantSettingService.update(DeliverySettings.DELIVERY_BUCKET, "not-existing-bucket");
        DeliveryAndJob deliveryAndJob = initDeliveryRequestAndJob();
        JobInfo jobInfo = deliveryAndJob.getJobInfo();

        // WHEN
        this.getJobTestUtils().runAndWaitJob(jobInfo, 122225);

        // THEN
        // check job OrderDeliveryZipJob has failed
        Assertions.assertThat(jobInfoService.retrieveJob(jobInfo.getId()).getStatus().getStatus())
                  .isEqualTo(JobStatus.FAILED);
        // check workspace was deleted even in case of error
        Assertions.assertThat(workspaceService.getMicroserviceWorkspace().toFile()).isEmptyDirectory();
        // check request is in error deleted
        List<DeliveryRequest> requests = deliveryRequestRepository.findAll();
        Assertions.assertThat(requests).hasSize(1);
        DeliveryRequest requestInError = requests.get(0);
        Assertions.assertThat(requestInError.getStatus()).isEqualTo(DeliveryRequestStatus.ERROR);
        Assertions.assertThat(requestInError.getErrorType()).isEqualTo(DeliveryErrorType.INTERNAL_ERROR);
    }

    private void checkDeliveryResponseSent(List<ISubscribable> eventsSent, DeliveryRequestStatus finalReqStatus) {
        List<DeliveryResponseDtoEvent> response = eventsSent.stream()
                                                            .filter(event -> event instanceof DeliveryResponseDtoEvent)
                                                            .map(deliveryRes -> (DeliveryResponseDtoEvent) deliveryRes)
                                                            .toList();
        Assertions.assertThat(response).hasSize(1);
        DeliveryResponseDtoEvent deliveryResponse = response.get(0);
        Assertions.assertThat(deliveryResponse.getCorrelationId()).isEqualTo(DELIVERY_CORRELATION_ID);
        Assertions.assertThat(deliveryResponse.getStatus()).isEqualTo(finalReqStatus);
    }

    private void checkJobEventSent(List<ISubscribable> eventsSent, JobEventType finalJobStatus) {
        List<JobEvent> responses = eventsSent.stream()
                                             .filter(event -> event instanceof JobEvent)
                                             .map(deliveryRes -> (JobEvent) deliveryRes)
                                             .toList();
        Assertions.assertThat(responses).hasSize(2);
        Assertions.assertThat(responses.get(0).getJobEventType()).isEqualTo(JobEventType.RUNNING);
        Assertions.assertThat(responses.get(1).getJobEventType()).isEqualTo(finalJobStatus);
    }

    public void checkZipDeliveryExistsOnS3() throws MalformedURLException {
        StorageConfig storageConfig = getStorageConfig();

        StorageCommandID cmdId = new StorageCommandID("test-exists-S3-delivery", UUID.randomUUID());
        String expectedZipPath = DELIVERY_CORRELATION_ID + "/" + String.format(MULTIPLE_FILES_ZIP_NAME_PATTERN,
                                                                               DELIVERY_CORRELATION_ID);
        StorageCommand.Check checkCmd = StorageCommand.check(storageConfig, cmdId, expectedZipPath);
        try (S3HighLevelReactiveClient s3Client = getS3Client()) {
            StorageCommandResult.CheckResult checkResult = s3Client.check(checkCmd).block();
            assert checkResult != null;
            checkResult.matchCheckResult(present -> true,
                                         absent -> Assertions.fail(String.format(
                                             "S3 zip was not uploaded to S3 location '%s'.",
                                             expectedZipPath)),
                                         unreachableStorage -> {
                                             throw new S3ClientException(unreachableStorage.getThrowable());
                                         });
        }
    }

    private StorageConfig getStorageConfig() throws MalformedURLException {
        // Actually, delivery gets its S3 config from {@link DeliverySettingService} and not from StorageConfig
        return new StorageConfig.StorageConfigBuilder(getS3Server()).rootPath(DELIVERY_CORRELATION_ID).build();
    }

    private S3Server getS3Server() {
        try {
            return new S3Server(new URL(testS3.getScheme(), testS3.getHost(), testS3.getPort(), "").toString(),
                                testS3.getRegion(),
                                testS3.getKey(),
                                testS3.getSecret(),
                                testS3.getBucket());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private S3HighLevelReactiveClient getS3Client() {
        Scheduler scheduler = Schedulers.newParallel("test-delivery-s3-client", 10);
        return new S3HighLevelReactiveClient(scheduler, 2_000_000, 10);
    }

    private void clean() throws IOException {
        deliveryAndJobRepository.deleteAll();
        deliveryRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
        FileUtils.cleanDirectory(workspaceService.getMicroserviceWorkspace().toFile());
    }

    private DeliveryAndJob initDeliveryRequestAndJob() {
        DeliveryRequest deliveryRequest = deliveryRequestRepository.save(DeliveryStepUtils.buildDeliveryRequest());
        JobInfo jobInfo = jobInfoRepository.save(new JobInfo(false,
                                                             0,
                                                             Set.of(),
                                                             null,
                                                             OrderDeliveryZipJob.class.getName()));
        return deliveryAndJobRepository.save(new DeliveryAndJob(deliveryRequest, jobInfo));
    }

    private void initS3Settings(TestDeliveryServerProperties testedS3Config)
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        dynamicTenantSettingService.update(DeliverySettings.S3_SERVER,
                                           new S3DeliveryServer(testedS3Config.getScheme(),
                                                                testedS3Config.getHost(),
                                                                testedS3Config.getPort(),
                                                                testedS3Config.getRegion(),
                                                                testedS3Config.getKey(),
                                                                testedS3Config.getSecret()));
        dynamicTenantSettingService.update(DeliverySettings.DELIVERY_BUCKET, testedS3Config.getBucket());
    }

}
