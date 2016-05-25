/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 28/04/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cs.regards.tests.catalog;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import fr.cs.regards.RegardsIntegrationTests;
import fr.cs.regards.model.data.Dataset;

/**
 *
 * Test dataset flow (crud)
 *
 * @author msordi
 * @since 1.0-SNAPSHOT
 */
public class DatasetIT extends RegardsIntegrationTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetIT.class);

    private RestTemplate restTemplate_;

    @Value("${root.admin.login}")
    private String rootAdminLogin_;

    @Value("${root.admin.password}")
    private String rootAdminPassword_;

    @Before
    public void init() {
        if (restTemplate_ == null) {
            restTemplate_ = buildOauth2RestTemplate(rootAdminLogin_, rootAdminPassword_);
        }
    }

    // FIXME à revoir sans l'utilisation de JPA
    // @Test
    // public void createDataset() {
    // final Dataset dataset = new Dataset();
    // dataset.setName("my first dataset");
    // final ResponseEntity<Dataset> response = restTemplate_.postForEntity(getApiEndpoint()
    // .concat("admin/catalog/dataset"),
    // dataset, Dataset.class);
    //
    // Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    // final Dataset retrievedDataset = response.getBody();
    // Assert.assertNotNull(retrievedDataset);
    // Assert.assertNotNull(retrievedDataset.getId());
    // Assert.assertEquals(retrievedDataset.getName(), dataset.getName());
    // }

    @Test
    public void getDataset() {

        final ResponseEntity<Dataset> response = restTemplate_
                .getForEntity(getApiEndpoint().concat("catalog/datasets/{datasetName}"), Dataset.class, "a name");

        LOGGER.debug(response.getBody().getName());
    }

    @Test
    public void getAllDatasets() {
        final ParameterizedTypeReference<List<Dataset>> typeRef = new ParameterizedTypeReference<List<Dataset>>() {
        };
        @SuppressWarnings("unused")
        final ResponseEntity<List<Dataset>> response = restTemplate_.exchange(getApiEndpoint()
                                                                              .concat("catalog/datasets"),
                                                                              HttpMethod.GET, null, typeRef);
        // FIXME à revoir
        // LOGGER.debug(String.valueOf(response.getBody().size()));
    }
}
