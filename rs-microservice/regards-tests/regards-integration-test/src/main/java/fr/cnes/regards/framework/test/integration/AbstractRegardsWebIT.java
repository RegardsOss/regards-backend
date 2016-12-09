/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 *
 * Class AbstractRegardsWebIT
 *
 * Overides AbstractRegardsIT to change the WebEnvironement from Mock to defnied port.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public abstract class AbstractRegardsWebIT extends AbstractRegardsIT {

}
