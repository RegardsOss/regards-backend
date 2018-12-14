package fr.cnes.regards.framework.microservice.rest.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.cnes.regards.framework.microservice.rest.test.domain.ConfigurationPojo;
import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.manager.ModuleReadinessReport;

/**
 * Some Dummy export/import for test purpose
 * @author Sylvain VISSIERE-GUERINET
 */
public class TestConfigurationManager extends AbstractModuleManager<Void> {

    private boolean totalFail;

    private boolean partialFail;

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ConfigurationPojo> pojos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pojos.add(new ConfigurationPojo("Configuration " + i));
        }
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        for (ConfigurationPojo pojo : pojos) {
            configurations.add(ModuleConfigurationItem.build(pojo));
        }
        return ModuleConfiguration.build(info, configurations);
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {
        Set<String> importErrors = new HashSet<>();
        List<ModuleConfigurationItem<?>> confElements = configuration.getConfiguration();
        if (totalFail) {
            for (ModuleConfigurationItem<?> confElement : confElements) {
                if (ConfigurationPojo.class.isAssignableFrom(confElement.getKey())) {
                    ConfigurationPojo pojo = confElement.getTypedValue();
                    importErrors.add("Fail for " + pojo.getAttribute());
                }
            }
            return importErrors;
        }
        if (partialFail) {
            for (int i = 0; i < confElements.size() / 2; i++) {
                if (ConfigurationPojo.class.isAssignableFrom(confElements.get(i).getKey())) {
                    ConfigurationPojo pojo = confElements.get(i).getTypedValue();
                    importErrors.add("Fail for " + pojo.getAttribute());
                }
            }
        }
        return importErrors;
    }

    public boolean isTotalFail() {
        return totalFail;
    }

    public void setTotalFail(boolean totalFail) {
        this.totalFail = totalFail;
    }

    public boolean isPartialFail() {
        return partialFail;
    }

    public void setPartialFail(boolean partialFail) {
        this.partialFail = partialFail;
    }

    @Override
    public ModuleReadinessReport<Void> isReady() {
        return new ModuleReadinessReport<>(true, null, null);
    }
}
