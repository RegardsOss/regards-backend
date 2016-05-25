/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 28/04/2015 : Creation
 *
 * END-VERSION-HISTORY
 */
package fr.cs.regards.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import fr.cs.regards.RegardsIntegrationTests;
import fr.cs.regards.model.administration.root.Project;
import fr.cs.regards.services.rest.common.resource.RegardsResponse;

/**
 *
 * Integration tests for root services.
 *
 * @author msordi
 * @since 1.0-SNAPSHOT
 */
public class RootServiceIT extends RegardsIntegrationTests {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(RootServiceIT.class);

    private RestTemplate restTemplate_;

    @Before
    public void init() {
        if (restTemplate_ == null) {
            restTemplate_ = buildOauth2RestTemplate("root_admin", "root_admin");
        }
    }

    @Test
    public void addProject() {
        final String projectName = "NE02";
        final Project project = new Project(projectName);
        final ResponseEntity<RegardsResponse> response = restTemplate_.postForEntity(getApiEndpoint()
                                                                                     .concat("root/project"),
                                                                                     project, RegardsResponse.class);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        final RegardsResponse regardsResponse = response.getBody();
        Assert.assertNotNull(regardsResponse);
        Assert.assertEquals(Boolean.TRUE, regardsResponse.getSuccess());
    }
}
