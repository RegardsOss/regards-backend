/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.integration;

import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.jpa.multitenant.test.DefaultTestConfiguration;

/**
 * Base class to realize integration tests using JWT and MockMvc and mocked Cots. Should hold all the configurations to
 * be considred by any of its children.
 *
 * @author svissier
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = { DefaultTestConfiguration.class, MockAmqpConfiguration.class })
public abstract class AbstractRegardsIT extends AbstractRegardsITWithoutMockedCots {

}
