package fr.cnes.regards.framework.modules.workspace.rest;

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = { "classpath:workspace.properties" })
public class WorkspaceControllerIT extends AbstractRegardsIT {


    @Test
    public void testMonitoring() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(WorkspaceController.BASE_PATH, requestBuilderCustomizer, "error occured");
    }
}
