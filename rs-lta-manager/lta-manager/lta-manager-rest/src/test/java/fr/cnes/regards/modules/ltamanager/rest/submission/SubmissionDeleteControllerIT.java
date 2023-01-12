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

import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.dao.submission.SubmissionRequestSpecificationBuilder;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.search.SearchSubmissionRequestParameters;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.rest.submission.utils.SubmissionInfo;
import fr.cnes.regards.modules.ltamanager.rest.submission.utils.SubmissionRequestHelper;
import fr.cnes.regards.modules.model.client.IModelClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Test for {@link SubmissionDeleteController}
 *
 * @author tguillou
 */
@TestPropertySource(locations = { "classpath:application-test.properties" },
    properties = { "spring.jpa.properties.hibernate.default_schema=submission_delete_controller_it" })
public class SubmissionDeleteControllerIT extends AbstractRegardsIT {

    private static final String DEFAULT_OWNER = "owner";

    private static final String OTHER_OWNER = "ownerOfOnlyOneRequest";

    private static final String DEFAULT_SESSION = "session";

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
        requestRepository.deleteAll();
    }

    @Test
    public void test_invalidSearchCriteria() {
        // GIVEN
        SearchSubmissionRequestParameters searchParameters = createSubmissionRequestSearchParameters(DEFAULT_OWNER);
        searchParameters.setStatusesRestriction(new ValuesRestriction<SubmissionRequestState>().withInclude(List.of(
            SubmissionRequestState.GENERATION_PENDING)));
        this.performDefaultDelete(AbstractSubmissionController.ROOT_PATH,
                                  searchParameters,
                                  customizer().expectStatusBadRequest()
                                              .expect(MockMvcResultMatchers.content()
                                                                           .string(
                                                                               "Only final status (GENERATION_ERROR, DONE, INGESTION_ERROR) are allowed in status search criterion.")),
                                  "Error while creating deletion request");

        searchParameters.setStatusesRestriction(new ValuesRestriction<SubmissionRequestState>().withExclude(List.of(
            SubmissionRequestState.DONE)));
        this.performDefaultDelete(AbstractSubmissionController.ROOT_PATH,
                                  searchParameters,
                                  customizer().expectStatusBadRequest()
                                              .expect(MockMvcResultMatchers.content()
                                                                           .string(
                                                                               "Only include mode is allowed in status search criterion.")),
                                  "Error while creating deletion request");

        searchParameters.setStatusesRestriction(new ValuesRestriction<SubmissionRequestState>().withInclude(List.of(
            SubmissionRequestState.getAllFinishedState())));
        this.performDefaultDelete(AbstractSubmissionController.ROOT_PATH,
                                  searchParameters,
                                  customizer().expectStatusOk(),
                                  "Error while creating deletion request");
    }

    @Test
    public void test_endpointSuccess() throws Exception {
        // GIVEN
        createDefaultSubmissionsRequests();
        SearchSubmissionRequestParameters searchParameters = createSubmissionRequestSearchParameters(DEFAULT_OWNER);
        PageRequest page = PageRequest.of(0, 5);
        Specification<SubmissionRequest> spec = new SubmissionRequestSpecificationBuilder().withParameters(
            searchParameters).build();

        // PRE-TEST
        Page<SubmissionRequest> before = requestRepository.findAll(spec, page);
        Assert.assertEquals(3, before.getTotalElements());

        // WHEN
        ResultActions response = this.performDefaultDelete(AbstractSubmissionController.ROOT_PATH,
                                                           searchParameters,
                                                           customizer().expectStatusOk(),
                                                           "Error while creating deletion request");
        waitForJobEnd();
        // THEN
        response.andExpect(MockMvcResultMatchers.status().isOk());
        Page<SubmissionRequest> after = requestRepository.findAll(spec, page);
        Assert.assertEquals(0, after.getTotalElements());
    }

    private static void waitForJobEnd() throws InterruptedException {
        Thread.sleep(3000);
    }

    /// HELPERS

    private void createDefaultSubmissionsRequests() {
        IntStream.range(0, 3)
                 .forEach(i -> createAndSaveSubmissionRequest(DEFAULT_OWNER, OffsetDateTime.now().minusDays(1)));
        createAndSaveSubmissionRequest(OTHER_OWNER, OffsetDateTime.now().minusDays(1));
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
