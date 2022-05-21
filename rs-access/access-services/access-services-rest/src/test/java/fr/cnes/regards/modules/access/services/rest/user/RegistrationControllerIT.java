package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Random;
import java.util.UUID;

import static fr.cnes.regards.modules.access.services.rest.user.mock.ProjectUsersClientMock.*;
import static fr.cnes.regards.modules.access.services.rest.user.mock.RegistrationClientMock.ACCESS_REQUEST_STUB;

/**
 * Integration tests for Registration REST Controller.
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=access" },
    locations = { "classpath:application-test.properties" })
public class RegistrationControllerIT extends AbstractRegardsTransactionalIT {

    private static final String TOKEN_STUB = UUID.randomUUID().toString();

    private static final Long ACCESS_ID_STUB = (long) new Random().nextInt(10_000);

    @Override
    protected String getDefaultRole() {
        return DefaultRole.PROJECT_ADMIN.toString();
    }

    @Test
    public void requestAccess() {
        RequestBuilderCustomizer customizer = customizer().expectStatusCreated();

        performDefaultPost(RegistrationController.REQUEST_MAPPING_ROOT,
                           ACCESS_REQUEST_STUB,
                           customizer,
                           "Failed to request access");
    }

    @Test
    public void requestExternalAccess() {
        RequestBuilderCustomizer customizer = customizer().expectStatusCreated();

        performDefaultPost(RegistrationController.REQUEST_MAPPING_ROOT,
                           ACCESS_REQUEST_STUB,
                           customizer,
                           "Failed to request external access");
    }

    @Test
    public void verifyEmail() {
        String api = RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.VERIFY_EMAIL_RELATIVE_PATH;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultGet(api, customizer, "Failed to verify email", TOKEN_STUB);
    }

    @Test
    public void acceptAccessRequest() {
        String api = RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.ACCEPT_ACCESS_RELATIVE_PATH;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultPut(api, null, customizer, "Failed to accept access request", ACCESS_ID_STUB);
    }

    @Test
    public void denyAccessRequest() {
        String api = RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.DENY_ACCESS_RELATIVE_PATH;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultPut(api, null, customizer, "Failed to deny access request", ACCESS_ID_STUB);
    }

    @Test
    public void activeAccess() {
        String api = RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.ACTIVE_ACCESS_RELATIVE_PATH;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultPut(api, null, customizer, "Failed to activate inactive user", ACCESS_ID_STUB);
    }

    @Test
    public void inactiveAccess() {
        String api = RegistrationController.REQUEST_MAPPING_ROOT + RegistrationController.INACTIVE_ACCESS_RELATIVE_PATH;

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultPut(api, null, customizer, "Failed to deactivate active user", ACCESS_ID_STUB);
    }

    @Test
    public void removeAccessRequest() {
        String api = RegistrationController.REQUEST_MAPPING_ROOT + "/{access_id}";

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();

        performDefaultDelete(api, customizer, "Failed to reject access request", ACCESS_ID_STUB);
    }

    protected RequestBuilderCustomizer expectSingleUserFromClientMock(RequestBuilderCustomizer customizer) {
        return customizer.expectValue("$.content.email", PROJECT_USER_STUB_EMAIL);
    }

    protected RequestBuilderCustomizer expectPagedUserFromClientMock(RequestBuilderCustomizer customizer) {
        return customizer.expectValue("$.content.[0].content.email", PROJECT_USER_STUB_EMAIL);
    }

    private RequestBuilderCustomizer expectPagingFromClientMock(RequestBuilderCustomizer customizer) {
        return customizer.expectValue("$.metadata.totalElements", TOTAL_ELEMENTS_STUB)
                         .expectValue("$.metadata.totalPages", TOTAL_PAGES_STUB)
                         .expectValue("$.metadata.number", PAGE_NUMBER_STUB);
    }
}
