/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.authentication.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@ContextConfiguration(classes = BorrowRoleITConfiguration.class)
@TestPropertySource(locations = { "classpath:application-test.properties" })
public class BorrowRoleControllerIT extends AbstractRegardsIT {

    private static final Logger LOG = LoggerFactory.getLogger(BorrowRoleControllerIT.class);

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Test
    public void testSwitch() {
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(BorrowRoleController.PATH_BORROW_ROLE + BorrowRoleController.PATH_BORROW_ROLE_TARGET,
                          expectations, "ERROR", DefaultRole.PUBLIC.toString());
    }

}
