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
package fr.cnes.regards.modules.ltamanager.rest.submission;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SubmissionRequestSearchParameters;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.rest.submission.utils.SubmissionInfo;
import fr.cnes.regards.modules.ltamanager.rest.submission.utils.SubmissionRequestHelper;
import fr.cnes.regards.modules.model.client.IModelClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;

/**
 * Test for {@link SubmissionReadController}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(locations = { "classpath:application-test.properties" },
                    properties = { "spring.jpa.properties.hibernate.default_schema=submission_read_controller_it" })
public class SubmissionReadControllerIT extends AbstractRegardsIT {

    private static final String OWNER_1 = "Owner 1";

    private static final String OWNER_2 = "Owner 2";

    private static final String SESSION_1 = "Session 1";

    public static final String DATATYPE_ERROR = "datatypeError";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ISubmissionRequestRepository requestRepository;

    @Autowired
    private SubmissionRequestHelper submissionRequestHelper;

    @MockBean
    private IModelClient modelClient;

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
        cleanRepositories();
    }

    private void cleanRepositories() {
        requestRepository.deleteAll();
    }

    @Test
    @Purpose("Test if state info of an existing request is successfully returned")
    public void read_request_state_success() throws Exception {
        // GIVEN
        SubmissionRequest submissionRequest = submissionRequestHelper.createAndSaveSubmissionRequest(new SubmissionInfo(
            "ownerInfo",
            "sessionInfo",
            EntityType.DATA.toString(),
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            SubmissionRequestState.GENERATION_ERROR));

        // WHEN
        ResultActions response = performDefaultGet(AbstractSubmissionController.ROOT_PATH
                                                   + SubmissionReadController.REQUEST_INFO_MAPPING,
                                                   customizer().expectStatusOk(),
                                                   "State should be returned",
                                                   submissionRequest.getRequestId());

        // THEN
        // expect state of the request to be returned
        response.andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.requestId",
                                                          equalTo(submissionRequest.getRequestId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.productId",
                                                          equalTo(submissionRequest.getProduct().getId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.status",
                                                          equalTo(submissionRequest.getStatus().toString())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.statusDate",
                                                          equalTo(OffsetDateTimeAdapter.format(submissionRequest.getStatusDate()))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.session", equalTo(submissionRequest.getSession())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.message",
                                                          equalTo(submissionRequest.getMessage())));
    }

    @Test
    @Purpose("Test if 404 http status is returned if no corresponding request was found")
    public void read_request_state_in_error_404() {
        // GIVEN
        // no existing request

        // WHEN / THEN
        // expect status not found
        performDefaultGet(AbstractSubmissionController.ROOT_PATH + SubmissionReadController.REQUEST_INFO_MAPPING,
                          customizer().expectStatusNotFound(),
                          "404 not found should be returned",
                          UUID.randomUUID());
    }

    @Test
    public void search_requests_by_metadataCriterion() throws Exception {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        List<SubmissionRequest> requests = initSearchCriterionData(now);
        // WHEN
        SubmissionRequestSearchParameters searchCriterionWithMetadata = new SubmissionRequestSearchParameters(OWNER_2,
                                                                                                              SESSION_1,
                                                                                                              DATATYPE_ERROR,
                                                                                                              null,
                                                                                                              null,
                                                                                                              null,
                                                                                                              null);

        ResultActions response = performDefaultPost(AbstractSubmissionController.ROOT_PATH
                                                    + SubmissionReadController.SEARCH_MAPPING,
                                                    searchCriterionWithMetadata,
                                                    customizer().expectStatusOk(),
                                                    "Error creating request dto");
        // THEN
        response.andExpect(MockMvcResultMatchers.jsonPath("$.metadata").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", equalTo(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].content.requestId",
                                                          equalTo(requests.get(3).getRequestId())));
    }

    @Test
    public void search_requests_by_dateCriterion() throws Exception {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        List<SubmissionRequest> requests = initSearchCriterionData(now);
        // WHEN
        SubmissionRequestSearchParameters searchCriterionWithDates = new SubmissionRequestSearchParameters(null,
                                                                                                           null,
                                                                                                           null,
                                                                                                           new DatesRangeRestriction(
                                                                                                               now.plusSeconds(
                                                                                                                   1),
                                                                                                               now),
                                                                                                           new DatesRangeRestriction(
                                                                                                               now.plusSeconds(
                                                                                                                   3),
                                                                                                               now),
                                                                                                           null,
                                                                                                           null);

        ResultActions response = performDefaultPost(AbstractSubmissionController.ROOT_PATH
                                                    + SubmissionReadController.SEARCH_MAPPING,
                                                    searchCriterionWithDates,
                                                    customizer().expectStatusOk(),
                                                    "Error creating request dto");
        // THEN
        response.andExpect(MockMvcResultMatchers.jsonPath("$.metadata").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", equalTo(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].content.requestId",
                                                          equalTo(requests.get(2).getRequestId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[1].content.requestId",
                                                          equalTo(requests.get(0).getRequestId())));
    }

    @Test
    public void search_requests_by_statusCriterion() throws Exception {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        List<SubmissionRequest> requests = initSearchCriterionData(now);
        // WHEN
        SubmissionRequestSearchParameters searchCriterionWithStatus = new SubmissionRequestSearchParameters(null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            null,
                                                                                                            new ValuesRestriction<>(
                                                                                                                List.of(
                                                                                                                    SubmissionRequestState.INGESTION_ERROR),
                                                                                                                ValuesRestrictionMode.INCLUDE),
                                                                                                            null);

        ResultActions response = performDefaultPost(AbstractSubmissionController.ROOT_PATH
                                                    + SubmissionReadController.SEARCH_MAPPING,
                                                    searchCriterionWithStatus,
                                                    customizer().expectStatusOk(),
                                                    "Error creating request dto");
        // THEN
        response.andExpect(MockMvcResultMatchers.jsonPath("$.metadata").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", equalTo(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].content.requestId",
                                                          equalTo(requests.get(3).getRequestId())));
    }

    @Test
    public void search_requests_by_IdsCriterion() throws Exception {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        List<SubmissionRequest> requests = initSearchCriterionData(now);
        // WHEN
        SubmissionRequestSearchParameters searchCriterionWithIds = new SubmissionRequestSearchParameters(null,
                                                                                                         null,
                                                                                                         null,
                                                                                                         null,
                                                                                                         null,
                                                                                                         null,
                                                                                                         new ValuesRestriction<>(
                                                                                                             List.of(
                                                                                                                 requests.get(
                                                                                                                             1)
                                                                                                                         .getRequestId()),
                                                                                                             ValuesRestrictionMode.EXCLUDE));

        ResultActions response = performDefaultPost(AbstractSubmissionController.ROOT_PATH
                                                    + SubmissionReadController.SEARCH_MAPPING,
                                                    searchCriterionWithIds,
                                                    customizer().expectStatusOk(),
                                                    "Error creating request dto");
        // THEN
        response.andExpect(MockMvcResultMatchers.jsonPath("$.metadata").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", equalTo(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].content.requestId",
                                                          equalTo(requests.get(3).getRequestId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[1].content.requestId",
                                                          equalTo(requests.get(2).getRequestId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[2].content.requestId",
                                                          equalTo(requests.get(0).getRequestId())));
    }

    @Test
    public void search_requests_by_AllCriterion() throws Exception {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        List<SubmissionRequest> requests = initSearchCriterionData(now);
        // WHEN
        SubmissionRequestSearchParameters searchCriterionWithAllCriterion = new SubmissionRequestSearchParameters(
            OWNER_1,
            SESSION_1,
            EntityType.DATA.toString(),
            new DatesRangeRestriction(now.plusSeconds(10), now),
            new DatesRangeRestriction(now.plusSeconds(10), now),
            new ValuesRestriction<>(List.of(SubmissionRequestState.GENERATED), ValuesRestrictionMode.INCLUDE),
            new ValuesRestriction<>(List.of(requests.get(0).getRequestId()), ValuesRestrictionMode.EXCLUDE));

        ResultActions response = performDefaultPost(AbstractSubmissionController.ROOT_PATH
                                                    + SubmissionReadController.SEARCH_MAPPING,
                                                    searchCriterionWithAllCriterion,
                                                    customizer().expectStatusOk(),
                                                    "Error creating request dto");
        // THEN
        response.andExpect(MockMvcResultMatchers.jsonPath("$.metadata").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", equalTo(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].content.requestId",
                                                          equalTo(requests.get(1).getRequestId())));
    }

    @Test
    public void search_requests_by_NoCriterion() throws Exception {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        List<SubmissionRequest> requests = initSearchCriterionData(now);
        // WHEN
        SubmissionRequestSearchParameters searchCriterionWithNoCriterion = new SubmissionRequestSearchParameters(null,
                                                                                                                 null,
                                                                                                                 null,
                                                                                                                 null,
                                                                                                                 null,
                                                                                                                 null,
                                                                                                                 null);

        ResultActions response = performDefaultPost(AbstractSubmissionController.ROOT_PATH
                                                    + SubmissionReadController.SEARCH_MAPPING
                                                    + "?page=0&size=20&sort=submissionStatus_statusDate,"
                                                    + "DESC&sort=submissionStatus_creationDate,DESC",
                                                    searchCriterionWithNoCriterion,
                                                    customizer().expectStatusOk(),
                                                    "Error creating request dto");
        // THEN
        response.andExpect(MockMvcResultMatchers.jsonPath("$.metadata").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", equalTo(4)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].content.requestId",
                                                          equalTo(requests.get(3).getRequestId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[1].content.requestId",
                                                          equalTo(requests.get(1).getRequestId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[2].content.requestId",
                                                          equalTo(requests.get(2).getRequestId())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[3].content.requestId",
                                                          equalTo(requests.get(0).getRequestId())));
    }

    private List<SubmissionRequest> initSearchCriterionData(OffsetDateTime now) {
        SubmissionRequest reqGenerated = submissionRequestHelper.createAndSaveSubmissionRequest(new SubmissionInfo(
            OWNER_1,
            SESSION_1,
            EntityType.DATA.toString(),
            now,
            now.plusSeconds(1),
            SubmissionRequestState.GENERATED));

        SubmissionRequest reqGenerated2 = submissionRequestHelper.createAndSaveSubmissionRequest(new SubmissionInfo(
            OWNER_1,
            SESSION_1,
            EntityType.DATA.toString(),
            now.plusSeconds(2),
            now.plusSeconds(3),
            SubmissionRequestState.GENERATED));

        SubmissionRequest reqPending = submissionRequestHelper.createAndSaveSubmissionRequest(new SubmissionInfo(OWNER_2,
                                                                                                                 SESSION_1,
                                                                                                                 EntityType.DATA.toString(),
                                                                                                                 now,
                                                                                                                 now.plusSeconds(
                                                                                                                     3),
                                                                                                                 SubmissionRequestState.INGESTION_PENDING));

        SubmissionRequest reqError = submissionRequestHelper.createAndSaveSubmissionRequest(new SubmissionInfo(OWNER_2,
                                                                                                               SESSION_1,
                                                                                                               DATATYPE_ERROR,
                                                                                                               now,
                                                                                                               now.plusSeconds(
                                                                                                                   6),
                                                                                                               SubmissionRequestState.INGESTION_ERROR));
        return List.of(reqGenerated, reqGenerated2, reqPending, reqError);
    }
}
