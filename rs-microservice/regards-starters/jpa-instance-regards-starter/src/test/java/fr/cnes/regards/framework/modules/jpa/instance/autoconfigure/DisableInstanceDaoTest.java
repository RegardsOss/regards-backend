/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jpa.instance.autoconfigure;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class DisableInstanceDaoTest
 *
 * Test class for JPA instance disactivation.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { DisableInstanceDaoTestConfiguration.class })
public class DisableInstanceDaoTest {

    /**
     *
     * Unit test to check JPA instance desactivation
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA instance desactivation")
    @Test
    public void checkSpringContext() {
        // Nothing to do
    }

}
