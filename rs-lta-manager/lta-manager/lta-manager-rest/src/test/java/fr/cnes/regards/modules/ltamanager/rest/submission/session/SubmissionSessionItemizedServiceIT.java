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
package fr.cnes.regards.modules.ltamanager.rest.submission.session;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionStatus;
import fr.cnes.regards.modules.ltamanager.rest.submission.utils.SubmissionRequestHelper;
import fr.cnes.regards.modules.model.client.IModelClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * @author Thomas GUILLOU
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=session_lta_it" })
public class SubmissionSessionItemizedServiceIT extends AbstractRegardsIT {

    private static final String SESSION_NAME = "sessionInfo";

    private static final String ANOTHER_SESSION_NAME = "anotherSessionName";

    private static final String ENDPOINT_FORMAT = "/sessions/%s/info/details?page={page}&size={size}";

    @MockBean
    private IModelClient iModelClient;

    @Autowired
    private SubmissionRequestHelper submissionRequestHelper;

    @Autowired
    private ISubmissionRequestRepository requestRepository;

    private String exploitToken;

    private String defaultEmail;

    @Before
    public void initialize() {
        requestRepository.deleteAll();
        defaultEmail = getDefaultUserEmail();
        exploitToken = jwtService.generateToken(getDefaultTenant(), defaultEmail, DefaultRole.EXPLOIT.toString());
    }

    @Test
    public void test_metadataSessionItemized() throws Exception {
        // GIVEN
        IntStream.range(0, 23).forEach(i -> createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.DONE));
        createAndSaveSubmissionRequest(ANOTHER_SESSION_NAME, SubmissionRequestState.GENERATION_PENDING);
        createAndSaveSubmissionRequest(ANOTHER_SESSION_NAME, SubmissionRequestState.INGESTION_ERROR);
        // WHEN
        String page = "1";
        String size = "5";
        ResultActions result = performGet(computeEndpointUrl(SESSION_NAME),
                                          exploitToken,
                                          customizer().expectStatusOk(),
                                          "Failed to get session",
                                          page,
                                          size);
        // THEN
        result.andExpect(jsonPath("$.metadata").exists())
              .andExpect(jsonPath("$.content.length()", is(5)))
              .andExpect(jsonPath("$.metadata.size", is(5)))
              .andExpect(jsonPath("$.metadata.totalElements", is(23)))
              .andExpect(jsonPath("$.metadata.totalPages", is(5)))
              .andExpect(jsonPath("$.metadata.number", is(1)))
              .andExpect(jsonPath("$.globalStatus", is(SessionStatus.DONE.toString())))
              .andExpect(jsonPath("$.content[0].content.session", is(SESSION_NAME)));
    }

    @Test
    public void test_statusErrorButNoErrorInPage() throws Exception {
        // GIVEN
        IntStream.range(0, 12).forEach(i -> createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.DONE));
        createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.INGESTION_ERROR);
        // WHEN
        String page = "1";
        String size = "5";
        ResultActions result = performGet(computeEndpointUrl(SESSION_NAME),
                                          exploitToken,
                                          customizer().expectStatusOk(),
                                          "Failed to get session",
                                          page,
                                          size);
        // THEN
        result.andExpect(jsonPath("$.content.length()", is(5)))
              .andExpect(jsonPath("$.metadata.totalElements", is(13)))
              .andExpect(jsonPath("$.globalStatus", is(SessionStatus.ERROR.toString())))
              .andExpect(jsonPath("$.content[0].content.status", is("DONE")))
              .andExpect(jsonPath("$.content[1].content.status", is("DONE")))
              .andExpect(jsonPath("$.content[2].content.status", is("DONE")))
              .andExpect(jsonPath("$.content[3].content.status", is("DONE")))
              .andExpect(jsonPath("$.content[4].content.status", is("DONE")));
    }
    
    @Test
    public void test_sessionCorrectInContent() throws Exception {
        // GIVEN
        IntStream.range(0, 5)
                 .forEach(i -> createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.GENERATION_PENDING));
        createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.DONE);
        IntStream.range(0, 6)
                 .forEach(i -> createAndSaveSubmissionRequest(ANOTHER_SESSION_NAME, SubmissionRequestState.DONE));
        // WHEN
        String page = "0";
        String size = "5";
        ResultActions result = performGet(computeEndpointUrl(SESSION_NAME),
                                          exploitToken,
                                          customizer().expectStatusOk(),
                                          "Failed to get session",
                                          page,
                                          size);
        // THEN
        result.andExpect(jsonPath("$.content.length()", is(5)))
              .andExpect(jsonPath("$.metadata.totalElements", is(6)))
              .andExpect(jsonPath("$.globalStatus", is(SessionStatus.RUNNING.toString())))
              .andExpect(jsonPath("$.content[0].content.session", is(SESSION_NAME)))
              .andExpect(jsonPath("$.content[1].content.session", is(SESSION_NAME)))
              .andExpect(jsonPath("$.content[2].content.session", is(SESSION_NAME)))
              .andExpect(jsonPath("$.content[3].content.session", is(SESSION_NAME)))
              .andExpect(jsonPath("$.content[4].content.session", is(SESSION_NAME)));
    }

    private void createAndSaveSubmissionRequest(String session, SubmissionRequestState state) {
        submissionRequestHelper.createAndSaveSubmissionRequest(defaultEmail, session, state);
    }

    private String computeEndpointUrl(String sessionName) {
        return String.format(ENDPOINT_FORMAT, sessionName);
    }
}
