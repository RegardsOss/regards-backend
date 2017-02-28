/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class DisableInstanceDaoTest
 *
 * Test class for JPA multitenant disactivation.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DisableMultitenantDaoTestConfiguration.class })
@DirtiesContext
public class DisableMultitenantDaoTest {

    /**
     *
     * Unit test to check JPA multitenant desactivation
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA multitenant desactivation")
    @Test
    public void checkSpringContext() {
        // Nothing to do
    }

}
