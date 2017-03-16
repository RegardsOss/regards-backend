/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest.contract;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.accessrights.rest.RegistrationController;

/**
 * @author Marc Sordi
 *
 */
@MultitenantTransactional
public class RegistrationContractIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationContractIT.class);

    @Test
    public void requestAccess() {

        // TODO read JSON
        String accessRequest = readJsonContract("request-access.json");

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isCreated());
        performDefaultPost(RegistrationController.REQUEST_MAPPING_ROOT, accessRequest, expectations,
                           "Access request error!");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
