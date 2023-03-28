package fr.cnes.regards.modules.configuration.rest;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.dao.IUIConfigurationRepository;
import fr.cnes.regards.modules.configuration.domain.ConfigurationDTO;
import fr.cnes.regards.modules.configuration.domain.UIConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertEquals;

/**
 * Class InstanceLayoutControllerIT
 * <p>
 * IT Tests for REST Controller
 *
 * @author Kevin Marchois
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class UIConfigurationControllerIT extends AbstractRegardsTransactionalIT {

    private final String APPLICATION_ID_TEST = "user";

    private final String CONFIGURATION_VALUE = "test";

    @Autowired
    private IUIConfigurationRepository uiConfigurationRepository;

    @Before
    public void setup() {
        uiConfigurationRepository.deleteAll();
    }

    @Test
    public void getNonExistingConfiguration() {
        performDefaultGet(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                          customizer().expectStatusNoContent(),
                          "Error message",
                          APPLICATION_ID_TEST);
    }

    @Test
    public void getConfiguration() {
        UIConfiguration toAdd = new UIConfiguration();
        toAdd.setId(1L);
        toAdd.setConfiguration(CONFIGURATION_VALUE);
        toAdd.setApplicationId(APPLICATION_ID_TEST);
        uiConfigurationRepository.save(toAdd);

        performDefaultGet(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                          customizer().expectStatusOk(),
                          "Error message",
                          APPLICATION_ID_TEST);
    }

    @Test
    public void addConfiguration() {
        UIConfiguration toAdd = new UIConfiguration();
        toAdd.setId(1L);
        toAdd.setConfiguration(CONFIGURATION_VALUE);
        toAdd.setApplicationId(APPLICATION_ID_TEST);

        performDefaultPost(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                           new ConfigurationDTO(CONFIGURATION_VALUE),
                           customizer().expectStatusOk(),
                           "Error message",
                           APPLICATION_ID_TEST);

        assertEquals(toAdd, uiConfigurationRepository.findAll().get(0));
    }

    @Test
    public void updateConfiguration() {
        performDefaultPost(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                           new ConfigurationDTO(CONFIGURATION_VALUE),
                           customizer().expectStatusOk(),
                           "Error message",
                           APPLICATION_ID_TEST);

        UIConfiguration toUpdate = uiConfigurationRepository.findByApplicationId(APPLICATION_ID_TEST).get(0);
        toUpdate.setConfiguration(CONFIGURATION_VALUE + CONFIGURATION_VALUE);

        performDefaultPut(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                          new ConfigurationDTO(CONFIGURATION_VALUE + CONFIGURATION_VALUE),
                          customizer().expectStatusOk(),
                          "Error message",
                          APPLICATION_ID_TEST);

        assertEquals(toUpdate, uiConfigurationRepository.findAll().get(0));
    }

    @Test
    public void updateNonExistingConfiguration() {
        performDefaultPut(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                          new ConfigurationDTO(CONFIGURATION_VALUE),
                          customizer().expectStatusNoContent(),
                          "Error message",
                          APPLICATION_ID_TEST);
    }
}
