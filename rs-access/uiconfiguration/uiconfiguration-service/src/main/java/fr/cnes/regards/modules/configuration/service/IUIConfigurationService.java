package fr.cnes.regards.modules.configuration.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;

/**
 * Class IConfigurationService
 * <p>
 * Interface for Configuration service
 *
 * @author Kevin Marchois
 */
@RegardsTransactional
public interface IUIConfigurationService {

    /**
     * Retrieve configuration for Front
     *
     * @param applicationId application id
     * @return String representation of configuration
     * @throws EntityNotFoundException if no configuration is found on database
     */
    public String retrieveConfiguration(String applicationId) throws EntityNotFoundException;

    /**
     * Add a configuration to the database if none exist
     *
     * @param configuration configuration to add
     * @return String representation of configuration
     */
    public String addConfiguration(String configuration, String applicationId);

    /**
     * Update the existing configuration
     *
     * @param configuration configuration to update
     * @return String representation of configuration
     * @throws EntityNotFoundException if we try to update a non existing configuration in database
     */
    public String updateConfiguration(String configuration, String applicationId) throws EntityNotFoundException;

}
