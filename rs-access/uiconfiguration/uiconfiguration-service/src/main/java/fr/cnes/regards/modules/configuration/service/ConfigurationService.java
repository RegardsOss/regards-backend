package fr.cnes.regards.modules.configuration.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.configuration.dao.ConfigurationRepository;
import fr.cnes.regards.modules.configuration.domain.Configuration;

@Service
@RegardsTransactional
public class ConfigurationService implements IConfigurationService {

	@Autowired 
	private ConfigurationRepository configurationRepo;
	
	@Override
	public Configuration retrieveConfiguration(String applicationId) throws EntityNotFoundException {
		List<Configuration> configurations = configurationRepo.findByApplicationId(applicationId);
		if (configurations.isEmpty()) {
			throw new EntityNotFoundException("No configuration founded");
		}
		return configurations.get(0);
	}

	@Override
	public Configuration addConfiguration(Configuration configuration) throws ModuleException {
		if (!configurationRepo.findByApplicationId(configuration.getApplicationId()).isEmpty()) {
			throw new ModuleException("Trying to add a new configuration but there is already an "
					+ "existing one for this application id");
		}
		
		return this.configurationRepo.save(configuration);
	}

	@Override
	public Configuration updateConfiguration(Configuration newConf) throws EntityNotFoundException{
		Configuration oldConfiguration = retrieveConfiguration(newConf.getApplicationId());
		if (!newConf.getId().equals(oldConfiguration.getId())) {
			throw new EntityNotFoundException("Trying to updata a non existing configuration");
		}
		return this.configurationRepo.save(newConf);
	}

}
