package fr.cnes.regards.modules.configuration.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.dao.IUIConfigurationRepository;
import fr.cnes.regards.modules.configuration.domain.UIConfiguration;
import fr.cnes.regards.modules.configuration.domain.ConfigurationDTO;

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
        performDefaultGet("/configuration/{applicationId}",
        		customizer().expectStatusNoContent(), "Error message", DEFAULT_APPLICATION_ID);
    }

	@Test
    public void getConfiguration() {
		UIConfiguration toAdd = new UIConfiguration();
		toAdd.setId(1L);
		toAdd.setConfiguration(CONFIGURATION_VALUE);
		toAdd.setApplicationId(DEFAULT_APPLICATION_ID);
		this.configurationRepo.save(toAdd);
        performDefaultGet("/configuration/{applicationId}",
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);
    }

	@Test
    public void addConfiguration() {
		UIConfiguration toAdd = new UIConfiguration();
		toAdd.setId(1L);
		toAdd.setConfiguration(CONFIGURATION_VALUE);
		toAdd.setApplicationId(DEFAULT_APPLICATION_ID);
        performDefaultPost("/configuration/{applicationId}", new ConfigurationDTO(CONFIGURATION_VALUE),
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);
        assertEquals(toAdd, configurationRepo.findAll().get(0));
    }

	@Test
    public void updateConfiguration() {
		performDefaultPost("/configuration/{applicationId}", new ConfigurationDTO(CONFIGURATION_VALUE),
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);

		UIConfiguration toUpdate = this.configurationRepo.findByApplicationId(DEFAULT_APPLICATION_ID).get(0);
		toUpdate.setConfiguration(CONFIGURATION_VALUE + CONFIGURATION_VALUE);

        performDefaultPut("/configuration/{applicationId}", new ConfigurationDTO(CONFIGURATION_VALUE + CONFIGURATION_VALUE),
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);
        assertEquals(toUpdate, configurationRepo.findAll().get(0));
    }

	@Test
    public void updateNonExistingConfiguration() {
		performDefaultPut("/configuration/{applicationId}", new ConfigurationDTO(CONFIGURATION_VALUE),
        		customizer().expectStatusNoContent(), "Error message", DEFAULT_APPLICATION_ID);
    }
}
