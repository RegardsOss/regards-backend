package fr.cnes.regards.modules.configuration.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.dao.IUIConfigurationRepository;
import fr.cnes.regards.modules.configuration.domain.ConfigurationDTO;
import fr.cnes.regards.modules.configuration.domain.UIConfiguration;
import static org.junit.Assert.assertEquals;

/**
 *
 * Class InstanceLayoutControllerIT
 *
 * IT Tests for REST Controller
 *
 * @author Kevin Marchois
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class UIConfigurationControllerIT extends AbstractRegardsTransactionalIT {

    private final String DEFAULT_APPLICATION_ID = "user";

    private final String CONFIGURATION_VALUE = "test";

    @Autowired
    private IUIConfigurationRepository configurationRepo;

    @Before
    public void setup() {
        configurationRepo.deleteAll();
    }

    @Test
    public void getNonExistingConfiguration() {
        performDefaultGet(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                          customizer().expectStatusNoContent(),
                          "Error message",
                          DEFAULT_APPLICATION_ID);
    }

    @Test
    public void getConfiguration() {
        UIConfiguration toAdd = new UIConfiguration();
        toAdd.setId(1L);
        toAdd.setConfiguration(CONFIGURATION_VALUE);
        toAdd.setApplicationId(DEFAULT_APPLICATION_ID);
        this.configurationRepo.save(toAdd);
        performDefaultGet(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                          customizer().expectStatusOk(),
                          "Error message",
                          DEFAULT_APPLICATION_ID);
    }

    @Test
    public void addConfiguration() {
        UIConfiguration toAdd = new UIConfiguration();
        toAdd.setId(1L);
        toAdd.setConfiguration(CONFIGURATION_VALUE);
        toAdd.setApplicationId(DEFAULT_APPLICATION_ID);
        performDefaultPost(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                           new ConfigurationDTO(CONFIGURATION_VALUE),
                           customizer().expectStatusOk(),
                           "Error message",
                           DEFAULT_APPLICATION_ID);
        assertEquals(toAdd, configurationRepo.findAll().get(0));
    }

    @Test
    public void updateConfiguration() {
        performDefaultPost(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                           new ConfigurationDTO(CONFIGURATION_VALUE),
                           customizer().expectStatusOk(),
                           "Error message",
                           DEFAULT_APPLICATION_ID);

        UIConfiguration toUpdate = this.configurationRepo.findByApplicationId(DEFAULT_APPLICATION_ID).get(0);
        toUpdate.setConfiguration(CONFIGURATION_VALUE + CONFIGURATION_VALUE);

        performDefaultPut(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                          new ConfigurationDTO(CONFIGURATION_VALUE + CONFIGURATION_VALUE),
                          customizer().expectStatusOk(),
                          "Error message",
                          DEFAULT_APPLICATION_ID);
        assertEquals(toUpdate, configurationRepo.findAll().get(0));
    }

    @Test
    public void updateNonExistingConfiguration() {
        performDefaultPut(UIConfigurationController.CONFIGURATION_PATH + UIConfigurationController.APPLICATION_ID_PATH,
                          new ConfigurationDTO(CONFIGURATION_VALUE),
                          customizer().expectStatusNoContent(),
                          "Error message",
                          DEFAULT_APPLICATION_ID);
    }
}
