/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.access.domain.LayoutDefaultApplicationIds;
import fr.cnes.regards.modules.access.domain.project.Layout;

/**
 *
 * Class InstanceLayoutControllerIT
 *
 * IT Tests for REST Controller
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class LayoutControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LayoutControllerIT.class);

    @Test
    public void getUserApplicationLayout() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet("/layouts/{applicationId}", expectations, "Plop",
                          LayoutDefaultApplicationIds.USER.toString());
    }

    @Test
    public void updateLayoutWithInvalidJsonFormat() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isUnprocessableEntity());
        final Layout layout = new Layout();
        layout.setId(1L);
        layout.setApplicationId("USER");
        layout.setLayout("{}}");
        performDefaultPut("/layouts/{applicationId}", layout, expectations, "Plop",
                          LayoutDefaultApplicationIds.USER.toString());
    }

    @Test
    public void updateLayout() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        final Layout layout = new Layout();
        layout.setId(1L);
        layout.setApplicationId("USER");
        layout.setLayout("{\"test\":\"ok\"}");
        performDefaultPut("/layouts/{applicationId}", layout, expectations, "Plop",
                          LayoutDefaultApplicationIds.USER.toString());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
