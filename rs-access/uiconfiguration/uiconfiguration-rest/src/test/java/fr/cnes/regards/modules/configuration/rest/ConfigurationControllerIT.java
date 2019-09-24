package fr.cnes.regards.modules.configuration.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.dao.ConfigurationRepository;
import fr.cnes.regards.modules.configuration.domain.Configuration;

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
public class ConfigurationControllerIT extends AbstractRegardsTransactionalIT {

	private final String DEFAULT_APPLICATION_ID = "user";
	private final String CONFIGURATION_VALUE = "test";

	@Autowired
	private ConfigurationRepository configurationRepo;
	
	@Before
	public void setup() {
		configurationRepo.deleteAll();
	}
	
	@Test
    public void getNonExistingConfiguration() {
        performDefaultGet("/configuration/{applicationId}", 
        		customizer().expectStatusNotFound(), "Error message", DEFAULT_APPLICATION_ID);
    }
	
	@Test
    public void getConfiguration() {
		Configuration toAdd = new Configuration();
		toAdd.setId(1L);
		toAdd.setConfiguration(CONFIGURATION_VALUE);
		toAdd.setApplicationId(DEFAULT_APPLICATION_ID);
		this.configurationRepo.save(toAdd);
        performDefaultGet("/configuration/{applicationId}", 
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);
    }
	
	@Test
    public void addConfiguration() {
		Configuration toAdd = new Configuration();
		toAdd.setId(1L);
		toAdd.setConfiguration(CONFIGURATION_VALUE);
		toAdd.setApplicationId(DEFAULT_APPLICATION_ID);
        performDefaultPost("/configuration/{applicationId}", toAdd,
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);
        assertEquals(toAdd, configurationRepo.findAll().get(0));
    }
	
	/**
	 * 
	 * Two post on adding method we will expect an error
	 */
	@Test
    public void addTwoConfigurations() {
		Configuration toAdd = new Configuration();
		toAdd.setId(1L);
		toAdd.setConfiguration(CONFIGURATION_VALUE);
		toAdd.setApplicationId(DEFAULT_APPLICATION_ID);
		// the first should work
        performDefaultPost("/configuration/{applicationId}", toAdd,
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);
        // the second should fail
        performDefaultPost("/configuration/{applicationId}", toAdd,
        		customizer().expectStatus(HttpStatus.INTERNAL_SERVER_ERROR), "Error message", DEFAULT_APPLICATION_ID);
    }
	
	@Test
    public void updateConfiguration() {
		Configuration toAdd = new Configuration();
		toAdd.setId(1L);
		toAdd.setConfiguration(CONFIGURATION_VALUE);
		toAdd.setApplicationId(DEFAULT_APPLICATION_ID);
		performDefaultPost("/configuration/{applicationId}", toAdd,
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);
		
		Configuration toUpdate = this.configurationRepo.findByApplicationId(DEFAULT_APPLICATION_ID).get(0);
		toUpdate.setConfiguration(CONFIGURATION_VALUE + CONFIGURATION_VALUE);

        performDefaultPut("/configuration/{applicationId}", toUpdate,
        		customizer().expectStatusOk(), "Error message", DEFAULT_APPLICATION_ID);
        assertEquals(toUpdate, configurationRepo.findAll().get(0));
    }
	
	@Test
    public void updateNonExistingConfiguration() {
		Configuration toUpdate= new Configuration();
		toUpdate.setId(1L);
		toUpdate.setConfiguration(CONFIGURATION_VALUE);
		toUpdate.setApplicationId(DEFAULT_APPLICATION_ID);
		performDefaultPut("/configuration/{applicationId}", toUpdate,
        		customizer().expectStatusNotFound(), "Error message", DEFAULT_APPLICATION_ID);
    }
}
