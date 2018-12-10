package fr.cnes.regards.framework.modules.workspace.rest;

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(properties = { "regards.workspace.occupation.threshold=1",
        "regards.cipher.key-location=src/test/resources/testKey", "regards.cipher.iv=1234567812345678",
        "spring.datasource.type=" })
public class WorkspaceControllerIT extends AbstractRegardsIT {

    @Test
    public void testMonitoring() {
        performDefaultGet(WorkspaceController.BASE_PATH, customizer().expectStatusOk(), "error occured");
    }
}
