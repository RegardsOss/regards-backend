/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 * Class AbstractRegardsWebIT
 *
 * Overides AbstractRegardsIT to change the WebEnvironement from Mock to defnied port.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@ContextConfiguration(classes = { DefaultTestFeignConfiguration.class })
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractRegardsWebIT extends AbstractRegardsIT {

    /**
     * Random port injection
     */
    @LocalServerPort
    private int port;

    protected int getPort() {
        return port;
    }
}
