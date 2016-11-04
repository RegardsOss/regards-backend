package fr.cnes.regards.modules.accessrights;

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
import fr.cnes.regards.modules.accessrights.client.IAccessesClient;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;

@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { AccessesClientTestConfiguration.class })
public class AccessesClientFallbacksTest {

    private static final Logger LOG = LoggerFactory.getLogger(AccessesClientFallbacksTest.class);

    @Autowired
    private IAccessesClient accessesClient;

    @Autowired
    private JWTService jwtService;

    @Test
    public void clientTest() {
        try {

            jwtService.injectToken("test", "ROLE");

            ResponseEntity<?> results = accessesClient.retrieveAccessRequestList();
            HttpStatus status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = accessesClient.acceptAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = accessesClient.denyAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = accessesClient.getAccessSettings();
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = accessesClient.removeAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = accessesClient.requestAccess(new AccessRequestDTO());
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = accessesClient.updateAccessSettings(null);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

    }

}
