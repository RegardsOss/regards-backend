package fr.cnes.regards.modules.accessRights;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.modules.accessRights.client.IAccessesClient;
import fr.cnes.regards.modules.accessRights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;

@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { AccessesClientTestConfiguration.class })
public class AccessesClientFallbacksTest {

    @Autowired
    private IAccessesClient accessesClient;

    @Test
    public void clientTest() {
        try {
            ResponseEntity<?> results = (ResponseEntity<List<Resource<ProjectUser>>>) accessesClient
                    .retrieveAccessRequestList();
            HttpStatus status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = (ResponseEntity<?>) accessesClient.acceptAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = (ResponseEntity<?>) accessesClient.denyAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = (ResponseEntity<?>) accessesClient.getAccessSettingList();
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = (ResponseEntity<?>) accessesClient.removeAccessRequest(0L);
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = (ResponseEntity<?>) accessesClient.requestAccess(new AccessRequestDTO());
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = (ResponseEntity<?>) accessesClient.updateAccessSetting("");
            status = results.getStatusCode();
            Assert.assertTrue("Fallback error", status.equals(HttpStatus.SERVICE_UNAVAILABLE));
        } catch (final Exception e) {
            Assert.fail(e.getMessage());
        }

    }

}
