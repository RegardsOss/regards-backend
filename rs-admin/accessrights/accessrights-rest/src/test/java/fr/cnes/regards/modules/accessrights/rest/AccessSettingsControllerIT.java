package fr.cnes.regards.modules.accessrights.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class AccessSettingsControllerIT extends AbstractRegardsIT{

    private static final String ERROR_MESSAGE = "Cannot reach model attributes";

    @Autowired
    private IAccessSettingsRepository accessSettingsRepository;

    /**
     * Check that the system allows to retrieve the access settings.
     */
    @Test
    @Purpose("Check that the system allows to retrieve the access settings.")
    public void getAccessSettings() {
        // Populate
        accessSettingsRepository.save(new AccessSettings());

        performDefaultGet(AccessSettingsController.REQUEST_MAPPING_ROOT, customizer().expectStatusOk(), ERROR_MESSAGE);
    }

    /**
     * Check that the system fails when trying to update a non existing access settings.
     */
    @MultitenantTransactional
    @Test
    @Purpose("Check that the system fails when trying to update a non existing access settings.")
    public void updateAccessSettingsEntityNotFound() {
        final AccessSettings settings = new AccessSettings();
        settings.setId(999L);

        performDefaultPut(AccessSettingsController.REQUEST_MAPPING_ROOT, settings, customizer().expectStatusNotFound(), "TODO Error message");
    }

    /**
     * Check that the system allows to update access settings in regular case.
     */
    @MultitenantTransactional
    @Test
    @Purpose("Check that the system allows to update access settings in regular case.")
    public void updateAccessSettings() {

        // First save settings
        final AccessSettings settings = new AccessSettings();
        accessSettingsRepository.save(settings);

        // Then update them
        settings.setMode("manual");

        performDefaultPut(AccessSettingsController.REQUEST_MAPPING_ROOT, settings, customizer().expectStatusOk(), "TODO Error message");
    }

}
