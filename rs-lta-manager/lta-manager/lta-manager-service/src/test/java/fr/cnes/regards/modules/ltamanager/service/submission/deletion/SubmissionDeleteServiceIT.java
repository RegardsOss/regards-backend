/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.dao.submission.SubmissionRequestSpecificationBuilder;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SearchSubmissionRequestParameters;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.service.deletion.SubmissionDeleteService;
import fr.cnes.regards.modules.ltamanager.service.submission.utils.SubmissionInfo;
import fr.cnes.regards.modules.ltamanager.service.submission.utils.SubmissionRequestHelper;
import fr.cnes.regards.modules.model.client.IModelClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.stream.IntStream;

/**
 * Test for {@link fr.cnes.regards.modules.ltamanager.service.deletion.SubmissionDeleteService}
 *
 * @author tguillou
 */
@ActiveProfiles(value = { "nojobs" })
@TestPropertySource(locations = { "classpath:application-test.properties" },
    properties = { "spring.jpa.properties.hibernate.default_schema=submission_delete_controller_it",
        "regards.ltamanager.request.deletion.batch.size=" + SubmissionDeleteServiceIT.BATCH_SIZE })
public class SubmissionDeleteServiceIT extends AbstractRegardsIT {

    private static final String DEFAULT_OWNER = "owner";

    private static final String OTHER_OWNER = "ownerOfOnlyOneRequest";

    private static final String DEFAULT_SESSION = "session";

    public static final int BATCH_SIZE = 5;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @SpyBean
    private ISubmissionRequestRepository requestRepository;

    @Autowired
    private SubmissionRequestHelper submissionRequestHelper;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private SubmissionDeleteService submissionDeleteService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IJobService jobService;

    @MockBean
    private IModelClient modelClient;

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
        requestRepository.deleteAll();
        // reset job info at each test
        jobInfoRepository.deleteAll();
        Mockito.reset(requestRepository);
    }

    @Test
    public void test_jobSuccessSinglePage() throws Exception {
        // GIVEN
        createDefaultSubmissionsRequests();
        SearchSubmissionRequestParameters searchParameters = createSubmissionRequestSearchParameters(DEFAULT_OWNER);
        PageRequest page = PageRequest.of(0, BATCH_SIZE);
        Specification<SubmissionRequest> spec = new SubmissionRequestSpecificationBuilder().withParameters(
            searchParameters).build();

        // PRE-TEST
        Page<SubmissionRequest> before = requestRepository.findAll(spec, page);
        Assert.assertEquals(5, before.getTotalElements());

        // WHEN
        JobInfo jobInfo = submissionDeleteService.scheduleRequestDeletionJob(searchParameters);
        jobService.runJob(jobInfo, runtimeTenantResolver.getTenant()).get();

        // THEN
        Page<SubmissionRequest> after = requestRepository.findAll(spec, page);
        Assert.assertEquals(0, after.getTotalElements());
        // 1 request remaining with another owner is remaining
        Assert.assertEquals(1, requestRepository.count());
        Mockito.verify(requestRepository, Mockito.times(1)).deleteAllInBatch(Mockito.any());
    }

    @Test
    public void test_jobSuccessMultiplePage() throws Exception {
        // GIVEN
        createDefaultSubmissionRequests(17);
        SearchSubmissionRequestParameters searchParameters = createSubmissionRequestSearchParameters(DEFAULT_OWNER);
        PageRequest page = PageRequest.of(0, BATCH_SIZE);
        Specification<SubmissionRequest> spec = new SubmissionRequestSpecificationBuilder().withParameters(
            searchParameters).build();

        // PRE-TEST
        Page<SubmissionRequest> before = requestRepository.findAll(spec, page);
        Assert.assertEquals(17, before.getTotalElements());

        // WHEN
        JobInfo jobInfo = submissionDeleteService.scheduleRequestDeletionJob(searchParameters);
        jobService.runJob(jobInfo, runtimeTenantResolver.getTenant()).get();

        // THEN
        Page<SubmissionRequest> after = requestRepository.findAll(spec, page);
        Assert.assertEquals(0, after.getTotalElements());
        // 4 pages (3 pages of 5 requests and 1 page of 2 request)
        Mockito.verify(requestRepository, Mockito.times(4)).deleteAllInBatch(Mockito.any());
    }

    @Test
    public void test_jobSuccessWithCreationDateEarly() throws Exception {
        // GIVEN
        createDefaultSubmissionsRequests();
        // add a SubmissionRequest that is created later than the job, so it must not be deleted.
        createAndSaveSubmissionRequest(DEFAULT_OWNER, OffsetDateTime.now().plusDays(1));
        SearchSubmissionRequestParameters searchParameters = createSubmissionRequestSearchParameters(DEFAULT_OWNER);
        PageRequest page = PageRequest.of(0, BATCH_SIZE);
        Specification<SubmissionRequest> spec = new SubmissionRequestSpecificationBuilder().withParameters(
            searchParameters).build();

        // PRE-TEST
        Page<SubmissionRequest> before = requestRepository.findAll(spec, page);
        Assert.assertEquals(6, before.getTotalElements());

        // WHEN
        JobInfo jobInfo = submissionDeleteService.scheduleRequestDeletionJob(searchParameters);
        jobService.runJob(jobInfo, runtimeTenantResolver.getTenant()).get();

        // THEN
        Page<SubmissionRequest> after = requestRepository.findAll(spec, page);
        Assert.assertEquals(1, after.getTotalElements());
    }

    /**
     * Check if only one job can be running at the same time
     */
    @Test
    public void test_jobConcurrence() throws ModuleException {
        // GIVEN
        SearchSubmissionRequestParameters searchParameters = createSubmissionRequestSearchParameters(DEFAULT_OWNER);
        // WHEN THEN
        JobInfo jobInfo = submissionDeleteService.scheduleRequestDeletionJob(searchParameters);
        Assert.assertNotNull("A job should be created", jobInfo);
        jobService.runJob(jobInfo, getDefaultTenant());
        Assertions.assertThrows(ModuleException.class,
                                () -> submissionDeleteService.scheduleRequestDeletionJob(searchParameters),
                                "Cannot schedule request deletion process : another deletion job is already running");
    }

    /// HELPERS

    private void createDefaultSubmissionsRequests() {
        createDefaultSubmissionRequests(5);
        createAndSaveSubmissionRequest(OTHER_OWNER, OffsetDateTime.now().minusDays(1));
    }

    private void createDefaultSubmissionRequests(int numberOfRequestToCreate) {
        IntStream.range(0, numberOfRequestToCreate)
                 .forEach(i -> createAndSaveSubmissionRequest(DEFAULT_OWNER, OffsetDateTime.now().minusDays(1)));
    }

    private SearchSubmissionRequestParameters createSubmissionRequestSearchParameters(String owner) {
        return new SearchSubmissionRequestParameters(owner, DEFAULT_SESSION, null, null, null, null, null);
    }

    private void createAndSaveSubmissionRequest(String owner, OffsetDateTime date) {
        submissionRequestHelper.createAndSaveSubmissionRequest(new SubmissionInfo(owner,
                                                                                  DEFAULT_SESSION,
                                                                                  EntityType.DATA.toString(),
                                                                                  date,
                                                                                  date,
                                                                                  SubmissionRequestState.DONE));
    }
}
