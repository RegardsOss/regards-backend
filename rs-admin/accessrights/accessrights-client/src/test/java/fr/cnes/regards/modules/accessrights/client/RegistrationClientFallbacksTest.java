package fr.cnes.regards.modules.accessrights.client;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { RegistrationClientTestConfiguration.class })
public class RegistrationClientFallbacksTest {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationClientFallbacksTest.class);

    @Autowired
    private IRegistrationClient registrationClient;

    @Autowired
    private JWTService jwtService;

    @Test
    public void clientTest() {
        try {

            jwtService.injectToken("test", "ROLE");

            ResponseEntity<?> results = registrationClient.retrieveAccessRequestList();
            HttpStatus status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = registrationClient.acceptAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = registrationClient.denyAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = registrationClient.getAccessSettings();
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = registrationClient.removeAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = registrationClient.requestAccess(new AccessRequestDto());
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = registrationClient.updateAccessSettings(null);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

    }

}
