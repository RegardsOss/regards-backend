/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class DefaultAuthorityProviderTest
 *
 * Test class for IAuthoritiesProvider default implemetation
 *
 * @author sbinda
 * @since 1.0-SNAPSHT
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class DefaultAuthorityProviderTest {

    /**
     * Default auhtorities provider defined in configuration class
     */
    @Autowired
    private IAuthoritiesProvider provider;

    /**
     *
     * defaultAuthorityProviderTest
     *
     * @throws SecurityException
     *             when no role with passed name could be found
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify access to all resources access per microservice")
    @Test
    public void defaultAuthorityProviderTest() throws SecurityException {

        // TODO
    }

}
