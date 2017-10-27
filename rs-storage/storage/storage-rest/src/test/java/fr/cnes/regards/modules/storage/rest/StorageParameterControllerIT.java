package fr.cnes.regards.modules.storage.rest;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RegardsTransactional
@TestPropertySource(locations = { "classpath:test.properties" })
@AutoConfigureRestDocs(outputDir = "target/snippets")
public class StorageParameterControllerIT extends AbstractRegardsTransactionalIT {

    @Before
    public void init() {

    }

    @Test
    public void testRetrieveAll() {
        List<ResultMatcher> expectations = Lists.newArrayList();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(StorageParameterController.ROOT_PATH, expectations, "retrieveAllStorageParameter");
    }
}
