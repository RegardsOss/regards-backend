package fr.cnes.regards.modules.processing.repository;

import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import io.vavr.control.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IRightsPluginConfigurationRepository extends JpaRepository<RightsPluginConfiguration, Long> {

    Option<RightsPluginConfiguration> findByPluginConfigurationId(Long pluginConfigurationId);

}
