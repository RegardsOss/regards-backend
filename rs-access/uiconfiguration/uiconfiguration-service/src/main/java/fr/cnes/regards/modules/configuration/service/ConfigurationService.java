package fr.cnes.regards.modules.configuration.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.annotation.Configurations;
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
	public String retrieveConfiguration(String applicationId) throws EntityNotFoundException {
		List<Configuration> configurations = configurationRepo.findByApplicationId(applicationId);
		if (configurations.isEmpty()) {
			throw new EntityNotFoundException("No configuration founded");
		}
		return configurations.get(0).getConfiguration();
	}

	@Override
	public String addConfiguration(String configuration, String applicationId){
		Configuration newConf = new Configuration();
		newConf.setApplicationId(applicationId);
		newConf.setConfiguration(configuration);
		return this.configurationRepo.save(newConf).getConfiguration();
	}

	@Override
	public String updateConfiguration(String newConf, String applicationId) throws EntityNotFoundException{
		List<Configuration> configurations = configurationRepo.findByApplicationId(applicationId);
		if (configurations.isEmpty()) {
			throw new EntityNotFoundException("No configuration founded");
		}
		Configuration conf = configurations.get(0);
		conf.setConfiguration(newConf);
		return this.configurationRepo.save(conf).getConfiguration();
	}

}
