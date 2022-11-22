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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.rest.submission.utils.SubmissionRequestHelper;
import fr.cnes.regards.modules.model.client.IModelClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

/**
 * @author Thomas GUILLOU
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=session_lta_it" })
public class SubmissionSessionServiceIT extends AbstractRegardsIT {

    private static final String SESSION_NAME = "sessionInfo";

    private static final String ANOTHER_SESSION_NAME = "anotherSessionName";

    private static final String ENDPOINT_FORMAT = "/sessions/%s/info";

    @MockBean
    private IModelClient iModelClient;

    @MockBean
    private IPublisher publisher;

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
    public void test_sessionDone() throws Exception {
        // GIVEN
        IntStream.range(0, 3).forEach(i -> createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.DONE));
        // WHEN
        ResultActions result = performGet(computeEndpointUrl(SESSION_NAME),
                                          exploitToken,
                                          customizer().expectStatusOk(),
                                          "Failed to get session");
        // THEN
        String expectedJsonOutput = getResourceAsString("sessions/session_done.json");
        result.andExpect(MockMvcResultMatchers.content().json(expectedJsonOutput));
    }

    @Test
    public void test_sessionRunning() throws Exception {
        // GIVEN
        IntStream.range(0, 3).forEach(i -> createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.DONE));
        createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.GENERATION_PENDING);
        // WHEN
        ResultActions result = performGet(computeEndpointUrl(SESSION_NAME),
                                          exploitToken,
                                          customizer().expectStatusOk(),
                                          "Failed to get session");
        // THEN
        String expectedJsonOutput = getResourceAsString("sessions/session_running.json");
        result.andExpect(MockMvcResultMatchers.content().json(expectedJsonOutput));
    }

    @Test
    public void test_sessionError() throws Exception {
        // GIVEN
        IntStream.range(0, 3).forEach(i -> createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.DONE));
        createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.GENERATION_PENDING);
        createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.INGESTION_ERROR);
        // WHEN
        ResultActions result = performGet(computeEndpointUrl(SESSION_NAME),
                                          exploitToken,
                                          customizer().expectStatusOk(),
                                          "Failed to get session");
        // THEN
        String expectedJsonOutput = getResourceAsString("sessions/session_error.json");
        result.andExpect(MockMvcResultMatchers.content().json(expectedJsonOutput));
    }

    @Test
    public void test_multipleSessionDone() throws Exception {
        // GIVEN
        IntStream.range(0, 3).forEach(i -> createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.DONE));
        createAndSaveSubmissionRequest(ANOTHER_SESSION_NAME, SubmissionRequestState.GENERATION_PENDING);
        createAndSaveSubmissionRequest(ANOTHER_SESSION_NAME, SubmissionRequestState.INGESTION_ERROR);
        // WHEN
        ResultActions result = performGet(computeEndpointUrl(SESSION_NAME),
                                          exploitToken,
                                          customizer().expectStatusOk(),
                                          "Failed to get session");
        // THEN
        String expectedJsonOutput = getResourceAsString("sessions/session_done.json");
        result.andExpect(MockMvcResultMatchers.content().json(expectedJsonOutput));
    }

    @Test
    public void test_sessionNotFound() {
        // GIVEN
        IntStream.range(0, 3).forEach(i -> createAndSaveSubmissionRequest(SESSION_NAME, SubmissionRequestState.DONE));
        createAndSaveSubmissionRequest(ANOTHER_SESSION_NAME, SubmissionRequestState.GENERATION_PENDING);
        createAndSaveSubmissionRequest(ANOTHER_SESSION_NAME, SubmissionRequestState.INGESTION_ERROR);
        String sessionNameUnknown = "SessionNameNotInBD";
        // WHEN THEN expect not found
        ResultActions result = performGet(computeEndpointUrl(sessionNameUnknown),
                                          exploitToken,
                                          customizer().expectStatusNotFound(),
                                          "Session " + sessionNameUnknown + " must not exists");
    }

    private String getResourceAsString(String path) throws IOException {
        InputStream resourceInputStream = SubmissionSessionServiceIT.class.getClassLoader().getResourceAsStream(path);
        return new String(resourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void createAndSaveSubmissionRequest(String session, SubmissionRequestState state) {
        submissionRequestHelper.createAndSaveSubmissionRequest(defaultEmail, session, state);
    }

    private String computeEndpointUrl(String sessionName) {
        return String.format(ENDPOINT_FORMAT, sessionName);
    }
}
