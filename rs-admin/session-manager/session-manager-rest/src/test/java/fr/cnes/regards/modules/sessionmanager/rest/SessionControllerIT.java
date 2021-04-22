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
package fr.cnes.regards.modules.sessionmanager.rest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.sessionmanager.dao.ISessionRepository;
import fr.cnes.regards.modules.sessionmanager.domain.Session;
import fr.cnes.regards.modules.sessionmanager.domain.SessionLifeCycle;
import fr.cnes.regards.modules.sessionmanager.domain.SessionState;
import fr.cnes.regards.modules.sessionmanager.domain.dto.UpdateSession;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;
import fr.cnes.regards.modules.sessionmanager.service.ISessionService;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=session")
@MultitenantTransactional
public class SessionControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionControllerIT.class);

    @Autowired
    public ISessionService sessionService;

    @Autowired
    public ISessionRepository sessionRepository;

    @Before
    public void clean() {
        sessionRepository.deleteAll();
    }

    public static List<FieldDescriptor> documentBody() {
        String prefixPath = "content[].content.";
        ConstrainedFields constrainedFields = new ConstrainedFields(Session.class);
        List<FieldDescriptor> descriptors = new ArrayList<>();
        descriptors.add(constrainedFields.withPath(prefixPath + "id", "id", "Session identifier", "Must be positive"));
        descriptors.add(constrainedFields.withPath(prefixPath + "source", "source", "Session source")
                .type(JSON_STRING_TYPE));
        descriptors.add(constrainedFields.withPath(prefixPath + "name", "name", "Session name").type(JSON_STRING_TYPE));
        descriptors.add(constrainedFields
                .withPath(prefixPath + "isLatest", "isLatest", "True when the session is the last of a source")
                .type(Boolean.class.toString()).optional().ignored());

        descriptors.add(constrainedFields
                .withPath(prefixPath + "creationDate", "creationDate", "Creation date of the session")
                .type(OffsetDateTime.class.getSimpleName()));
        descriptors.add(constrainedFields
                .withPath(prefixPath + "lastUpdateDate", "lastUpdateDate", "Last update date of the session")
                .type(OffsetDateTime.class.getSimpleName()));

        descriptors.add(constrainedFields
                .withPath(prefixPath + "state", "state", "Session state",
                          "Available values: " + Arrays.stream(SessionState.values()).map(type -> type.name())
                                  .reduce((first, second) -> first + ", " + second).get())
                .type(SessionState.class.getSimpleName()));

        descriptors.add(constrainedFields
                .withPath(prefixPath + "lifeCycle", "lifeCycle",
                          "Gathers all information about the session. The first level of this map represents the processing step, the second one stores a life cycle metric or information")
                .type(SessionLifeCycle.class.getSimpleName()));

        // ignore everything inside lifeCycle.key
        descriptors.add(constrainedFields.ignoreSubsectionWithPath(prefixPath + "lifeCycle.key"));

        // ignore metadata
        descriptors.add(constrainedFields.ignoreSubsectionWithPath("metadata"));
        // ignore links
        descriptors.add(constrainedFields.ignoreSubsectionWithPath("content[].links"));
        descriptors.add(constrainedFields.ignoreSubsectionWithPath("links"));

        return descriptors;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Find all sessions
     * @throws ModuleException module exception
     */
    @Test
    public void findAllSessions() {
        populateDatabase();

        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(9)));

        requestBuilderCustomizer.document(PayloadDocumentation.responseFields(documentBody()));

        requestBuilderCustomizer.document(RequestDocumentation
                .requestParameters(RequestDocumentation.parameterWithName("name").description("Session name").optional()
                        .attributes(Attributes
                                .key(RequestBuilderCustomizer.PARAM_TYPE).value(String.class.getSimpleName())),
                                   RequestDocumentation.parameterWithName("source").description("Session source")
                                           .optional()
                                           .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                   .value(String.class.getSimpleName())),
                                   RequestDocumentation.parameterWithName("from").description("Minimal creation date")
                                           .optional()
                                           .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                   .value(OffsetDateTime.class.getSimpleName())),
                                   RequestDocumentation.parameterWithName("to").description("Maximal creation date")
                                           .optional()
                                           .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                   .value(OffsetDateTime.class.getSimpleName())),
                                   RequestDocumentation.parameterWithName("state")
                                           .description(String.format("Session state.  Available values: "
                                                   + Arrays.stream(SessionState.values()).map(Enum::name)
                                                           .collect(Collectors.joining(", "))))
                                           .optional()
                                           .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                   .value(OffsetDateTime.class.getSimpleName())),
                                   RequestDocumentation.parameterWithName("onlyLastSession")
                                           .description("Keep only the latest session of source")
                                           .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                   .value(Boolean.class.getSimpleName()))));

        requestBuilderCustomizer.addParameter("name", "name").addParameter("source", "Source")
                .addParameter("from", OffsetDateTime.now().minusMonths(2).toString())
                .addParameter("to", OffsetDateTime.now().plusDays(2).toString())
                .addParameter("state", SessionState.OK.toString()).addParameter("onlyLastSession", "false");

        final ResultActions resultActions = performDefaultGet(SessionController.BASE_MAPPING, requestBuilderCustomizer,
                                                              "Should return result");

        assertMediaType(resultActions, MediaType.APPLICATION_JSON_UTF8);
        Assert.assertNotNull(payload(resultActions));
    }

    @Test
    public void findAllSessionNames() {
        populateDatabase();

        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(6)));

        requestBuilderCustomizer.document(RequestDocumentation.requestParameters(RequestDocumentation
                .parameterWithName("name").description("Session name").optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(String.class.getSimpleName()))));

        requestBuilderCustomizer.addParameter("name", "NA");

        final ResultActions resultActions = performDefaultGet(SessionController.BASE_MAPPING
                + SessionController.NAME_MAPPING, requestBuilderCustomizer, "Should return result");

        assertMediaType(resultActions, MediaType.APPLICATION_JSON_UTF8);
        Assert.assertNotNull(payload(resultActions));
    }

    @Test
    public void findAllSessionSource() {
        populateDatabase();

        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(5)));

        requestBuilderCustomizer.document(RequestDocumentation.requestParameters(RequestDocumentation
                .parameterWithName("source").description("Session source").optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(String.class.getSimpleName()))));

        requestBuilderCustomizer.addParameter("source", "sOU");

        final ResultActions resultActions = performDefaultGet(SessionController.BASE_MAPPING
                + SessionController.SOURCE_MAPPING, requestBuilderCustomizer, "Should return result");

        assertMediaType(resultActions, MediaType.APPLICATION_JSON_UTF8);
        Assert.assertNotNull(payload(resultActions));
    }

    @Test
    public void updateSession() {
        SessionMonitoringEvent sessionMonitoringEvent = SessionMonitoringEvent
                .build("session source", "session name", SessionNotificationState.ERROR, "key",
                       SessionNotificationOperator.REPLACE, "property", 1);
        Session sessionToUpdate = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent))
                .get(0);

        // Update the session status
        UpdateSession updateSession = new UpdateSession();
        updateSession.setState(SessionState.ACKNOWLEDGED);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.expect(MockMvcResultMatchers
                .jsonPath("$.content.state", Matchers.is(SessionState.ACKNOWLEDGED.toString())));

        requestBuilderCustomizer.document(RequestDocumentation.pathParameters(RequestDocumentation
                .parameterWithName("session_id").description("Session identifier").optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(Long.class.getSimpleName()))));

        ConstrainedFields constrainedFields = new ConstrainedFields(UpdateSession.class);
        List<FieldDescriptor> descriptors = new ArrayList<>();
        descriptors.add(constrainedFields
                .withPath("state", "state", "Session state. Allowed value: " + SessionState.ACKNOWLEDGED.toString()));

        requestBuilderCustomizer.document(PayloadDocumentation.requestFields(descriptors));

        final ResultActions resultActions = performDefaultPatch(SessionController.BASE_MAPPING
                + SessionController.SESSION_MAPPING, updateSession, requestBuilderCustomizer, "Should return result",
                                                                sessionToUpdate.getId());

        assertMediaType(resultActions, MediaType.APPLICATION_JSON_UTF8);
        Assert.assertNotNull(payload(resultActions));
    }

    @Test
    public void deleteSession() {
        SessionMonitoringEvent sessionMonitoringEvent = SessionMonitoringEvent
                .build("session source", "session name", SessionNotificationState.OK, "key",
                       SessionNotificationOperator.REPLACE, "property", 1);
        Session sessionToDelete = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent))
                .get(0);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.status().isOk());

        requestBuilderCustomizer
                .document(RequestDocumentation.pathParameters(RequestDocumentation.parameterWithName("force")
                        .description("When true, ask for database deletion too").optional()
                        .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                .value(boolean.class.getSimpleName())),
                                                              RequestDocumentation.parameterWithName("session_id")
                                                                      .description("Session identifier")
                                                                      .attributes(Attributes
                                                                              .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                              .value(Long.class.getSimpleName()))));

        performDefaultDelete(SessionController.BASE_MAPPING + SessionController.SESSION_MAPPING,
                             requestBuilderCustomizer, "Should return result", sessionToDelete.getId());
    }

    private void populateDatabase() {
        SessionMonitoringEvent sessionMonitoringEvent = SessionMonitoringEvent
                .build("Source", "Name", SessionNotificationState.OK, "key", SessionNotificationOperator.REPLACE,
                       "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source", "Name 2", SessionNotificationState.OK, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source", "Name 3", SessionNotificationState.OK, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source", "Name 4", SessionNotificationState.OK, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source", "Name 5", SessionNotificationState.OK, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source", "Name 6", SessionNotificationState.OK, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source", "Benennung 7", SessionNotificationState.OK,
                                                              "key", SessionNotificationOperator.REPLACE, "property",
                                                              1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source 2", "Name", SessionNotificationState.ERROR, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source 3", "Name", SessionNotificationState.OK, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source 4", "Name", SessionNotificationState.OK, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        sessionMonitoringEvent = SessionMonitoringEvent.build("Source 5", "Name", SessionNotificationState.OK, "key",
                                                              SessionNotificationOperator.REPLACE, "property", 1);
        sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
    }
}