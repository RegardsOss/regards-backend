/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.modules.configuration.dao.IModuleRepository;
import fr.cnes.regards.modules.configuration.domain.Module;

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
public class ModuleControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ModuleControllerIT.class);

    @Autowired
    private IModuleRepository repository;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private Module createModule(final boolean pActive, final boolean pDefault) {
        final Module module = new Module();
        module.setActive(pActive);
        module.setApplicationId("TEST");
        module.setConf("{\"test\":\"test\"}");
        module.setContainer("TestContainer");
        module.setDefaultDynamicModule(pDefault);
        module.setDescription("Description");
        module.setName("Module");
        return module;
    }

    @Before
    public void init() {

        final Module module = createModule(true, false);

        final Module module2 = createModule(false, true);

        repository.save(module);
        repository.save(module2);

    }

    @Test
    public void getUserApplicationModules() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(2)));
        performDefaultGet("/applications/{applicationId}/modules", expectations, "Plop", "TEST");

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(0)));
        performDefaultGet("/applications/{applicationId}/modules", expectations, "Plop", "TEST2");
    }

    @Test
    public void getUserApplicationActiveModules() {
        final RequestParamBuilder param = RequestParamBuilder.build().param("active", "true");
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(1)));
        performDefaultGet("/applications/{applicationId}/modules", expectations, "Plop", param, "TEST");
    }

    @Test
    public void saveNewModule() {
        final Module module = createModule(true, true);
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPost("/applications/{applicationId}/modules", module, expectations, "Plop", "TEST");

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(3)));
        performDefaultGet("/applications/{applicationId}/modules", expectations, "Plop", "TEST");
    }

}
