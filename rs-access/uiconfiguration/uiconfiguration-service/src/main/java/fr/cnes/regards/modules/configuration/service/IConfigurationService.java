package fr.cnes.regards.modules.configuration.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.configuration.domain.Configuration;

/**
*
* Class IConfigurationService
*
* Interface for Configuration service
*
* @author Kevin Marchois
*/
@RegardsTransactional
public interface IConfigurationService {

	/**
	 * 
	 * Retrieve configuration for Front
	 * 
	 * @param applicationId application id
     * @return {@link Configuration}
	 * @throws EntityNotFoundException if no configuration is found on database
	 */
	public Configuration retrieveConfiguration(String applicationId) throws EntityNotFoundException;
	
	/**
	 * 
	 * Add a configuration to the database if none exist 
	 * 
	 * @param configuration configuration to add
     * @return {@link Configuration}
	 * @throws ModuleException if a configuration already exists in database
	 */
	public Configuration addConfiguration(Configuration configuration) throws ModuleException;
	
	/**
	 * 
	 * Update the existing configuration
	 * 
	 * @param configuration configuration to update
     * @return {@link Configuration}
	 * @throws EntityNotFoundException if we try to update a non existing configuration in database
	 */
	public Configuration updateConfiguration(Configuration configuration) throws EntityNotFoundException;
	
}
