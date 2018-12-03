package fr.cnes.regards.framework.modules.workspace.rest;

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = { "classpath:workspace.properties" })
public class WorkspaceControllerIT extends AbstractRegardsIT {

    @Test
    public void testMonitoring() {
        performDefaultGet(WorkspaceController.BASE_PATH, customizer().expectStatusOk(), "error occured");
    }
}
