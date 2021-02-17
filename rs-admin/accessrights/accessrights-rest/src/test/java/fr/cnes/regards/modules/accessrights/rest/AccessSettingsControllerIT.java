package fr.cnes.regards.modules.accessrights.rest;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Collections;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class AccessSettingsControllerIT extends AbstractRegardsTransactionalIT {

    private static final String ERROR_MESSAGE = "Cannot reach model attributes";

    @Autowired
    private IAccessSettingsRepository accessSettingsRepository;

    @Autowired
    private IRoleRepository rolesRepository;

    @Before
    @After
    public final void setUp() {
        accessSettingsRepository.deleteAll();
    }

    /**
     * Check that the system allows to retrieve the access settings.
     */
    @Test
    @Purpose("Check that the system allows to retrieve the access settings.")
    public void getAccessSettings() {
        performDefaultGet(AccessSettingsController.REQUEST_MAPPING_ROOT, customizer().expectStatusOk(), ERROR_MESSAGE);
    }

    /**
     * Check that the system fails when trying to update a non existing access settings.
     */
    @Test
    @Purpose("Check that the system fails when trying to update a non existing access settings.")
    public void updateAccessSettingsEntityNotFound() {
        final AccessSettings settings = new AccessSettings();
        settings.setId(999L);

        performDefaultPut(AccessSettingsController.REQUEST_MAPPING_ROOT,
                          settings,
                          customizer().expectStatusNotFound(),
                          "TODO Error message");
    }

    /**
     * Check that the system allows to update access settings in regular case.
     */
    @Test
    @Purpose("Check that the system allows to update access settings in regular case.")
    public void updateAccessSettings() {
        performDefaultGet(AccessSettingsController.REQUEST_MAPPING_ROOT, customizer().expectStatusOk(), ERROR_MESSAGE);

        // First save settings
        final AccessSettings settings = accessSettingsRepository.findAll().get(0);
        settings.setMode("manual");
        settings.setDefaultRole(rolesRepository.findOneByName(DefaultRole.ADMIN.toString()).get());
        settings.setDefaultGroups(Collections.singletonList("plop"));

        performDefaultPut(AccessSettingsController.REQUEST_MAPPING_ROOT,
            settings,
            customizer().expectStatusOk(),
            "TODO Error message"
        );
    }

}
