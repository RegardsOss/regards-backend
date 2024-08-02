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
package fr.cnes.regards.modules.ltamanager.service.submission.deletion;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.integration.test.job.AbstractMultitenantServiceWithJobIT;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.settings.LtaSettings;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionStatus;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmittedProduct;
import fr.cnes.regards.modules.ltamanager.dto.submission.LtaDataType;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.model.client.IModelClient;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test for {@link SubmissionDeleteScheduler}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=submission_delete_it" })
@ActiveProfiles({ "noscheduler" })
public class SubmissionDeleteSchedulerIT extends AbstractMultitenantServiceWithJobIT {

    private static final int NB_REQUEST = 10;

    private static final Integer REQUEST_EXPIRY_IN_HOUR = 1;

    @Autowired
    private SubmissionDeleteExpiredService deleteService;

    @Autowired
    private ISubmissionRequestRepository requestRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IDynamicTenantSettingService settingService;

    @Autowired
    private IDynamicTenantSettingRepository settingRepository;

    @MockBean
    private IModelClient modelClient;

    @MockBean
    private IPublisher publisher;

    @Before
    public void init() {
        // clean repo
        jobInfoRepository.deleteAll();
        requestRepository.deleteAll();
        settingRepository.deleteAll();
    }

    @Test
    @Purpose("Test if expired submission requests are successfully deleted without configured expiration.")
    public void execute_delete_job_without_config() {
        deleteExpiredRequest(LtaSettings.DEFAULT_SUCCESS_EXPIRATION_HOURS + 2L);
    }

    @Test
    @Purpose("Test if expired submission requests are successfully deleted with configured expiration.")
    public void execute_delete_job_with_config()
        throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        int expirationDuration = 2;
        settingService.create(new DynamicTenantSetting(LtaSettings.SUCCESS_EXPIRATION_IN_HOURS_KEY,
                                                       "success expiration in hours",
                                                       expirationDuration));
        deleteExpiredRequest(expirationDuration + 2L);
    }

    @Test
    @Purpose("Test if no job is launched when there are no expired requests to deleted.")
    public void schedule_no_job() {
        // --- GIVEN ---
        initData(OffsetDateTime.now().minusHours(LtaSettings.DEFAULT_SUCCESS_EXPIRATION_HOURS).plusMinutes(1));

        // --- WHEN ---
        deleteService.scheduleJob();

        // --- THEN ---
        // no job has been created
        List<JobInfo> deleteJobInfos = this.getJobTestUtils().retrieveFullJobInfos(SubmissionDeleteExpiredJob.class);
        Assertions.assertThat(deleteJobInfos).isEmpty();
    }

    private void deleteExpiredRequest(long expirationOffset) {
        // --- GIVEN ---
        // expire the two first requests
        List<SubmissionRequest> savedRequests = initData(OffsetDateTime.now().minusHours(expirationOffset));

        // --- WHEN ---
        deleteService.scheduleJob();

        // --- THEN ---
        // Verify job have been created
        List<JobInfo> deleteJobInfos = this.getJobTestUtils().retrieveFullJobInfos(SubmissionDeleteExpiredJob.class);
        Assertions.assertThat(deleteJobInfos).hasSize(1);
        JobInfo deleteJob = deleteJobInfos.get(0);
        Assertions.assertThat(deleteJob.getStatus().getStatus()).isEqualTo(JobStatus.QUEUED);

        // Run job
        deleteJobInfos = this.getJobTestUtils().runAndWaitJob(deleteJobInfos, 5);
        deleteJobInfos.forEach(jobInfo -> Assertions.assertThat(deleteJob.getStatus().getStatus())
                                                    .isEqualTo(JobStatus.SUCCEEDED));

        // verify than only expired requests have been deleted
        // only two first requests are deleted
        savedRequests.remove(0);
        savedRequests.remove(0);

        Assertions.assertThat(requestRepository.findAll()).hasSameElementsAs(savedRequests);
    }

    private List<SubmissionRequest> initData(OffsetDateTime creationDate) {
        List<SubmissionRequest> submissionRequests = new ArrayList<>(NB_REQUEST);

        for (int i = 1; i <= NB_REQUEST; i++) {
            SubmissionStatus status = new SubmissionStatus(creationDate,
                                                           creationDate,
                                                           REQUEST_EXPIRY_IN_HOUR,
                                                           SubmissionRequestState.DONE,
                                                           null);
            SubmittedProduct product = new SubmittedProduct(EntityType.DATA.toString(),
                                                            "model",
                                                            Paths.get("/path/example"),
                                                            new SubmissionRequestDto("test req n°" + i,
                                                                                     UUID.randomUUID().toString(),
                                                                                     EntityType.DATA.toString(),
                                                                                     List.of(new ProductFileDto(
                                                                                         LtaDataType.RAWDATA,
                                                                                         "http://localhost/notexisting",
                                                                                         "example.raw",
                                                                                         "f016852239a8a919f05f6d2225c5aaca",
                                                                                         MediaType.APPLICATION_OCTET_STREAM))));
            SubmissionRequest submissionRequest = new SubmissionRequest(product.getProduct().getCorrelationId(),
                                                                        "owner",
                                                                        "session",
                                                                        false,
                                                                        status,
                                                                        product,
                                                                        null,
                                                                        null,
                                                                        null);
            submissionRequests.add(submissionRequest);
            creationDate = creationDate.plusHours(i);
        }
        return requestRepository.saveAll(submissionRequests);
    }

}
