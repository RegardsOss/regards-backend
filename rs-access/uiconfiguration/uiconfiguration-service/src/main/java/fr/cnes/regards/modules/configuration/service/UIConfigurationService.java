package fr.cnes.regards.modules.configuration.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IUIConfigurationRepository;
import fr.cnes.regards.modules.configuration.domain.UIConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RegardsTransactional
public class UIConfigurationService implements IUIConfigurationService {

    @Autowired
    private IUIConfigurationRepository configurationRepo;

    @Override
    public String retrieveConfiguration(String applicationId) throws EntityNotFoundException {
        List<UIConfiguration> UIConfigurations = configurationRepo.findByApplicationId(applicationId);
        if (UIConfigurations.isEmpty()) {
            throw new EntityNotFoundException("No configuration founded");
        }
        return UIConfigurations.get(0).getConfiguration();
    }

    @Override
    public String addConfiguration(String configuration, String applicationId) {
        UIConfiguration newConf = new UIConfiguration();
        newConf.setApplicationId(applicationId);
        newConf.setConfiguration(configuration);
        return this.configurationRepo.save(newConf).getConfiguration();
    }

    @Override
    public String updateConfiguration(String newConf, String applicationId) throws EntityNotFoundException {
        List<UIConfiguration> UIConfigurations = configurationRepo.findByApplicationId(applicationId);
        if (UIConfigurations.isEmpty()) {
            throw new EntityNotFoundException("No configuration founded");
        }
        UIConfiguration conf = UIConfigurations.get(0);
        conf.setConfiguration(newConf);
        return this.configurationRepo.save(conf).getConfiguration();
    }

}
