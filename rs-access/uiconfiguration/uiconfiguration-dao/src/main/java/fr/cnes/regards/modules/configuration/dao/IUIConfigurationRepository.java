package fr.cnes.regards.modules.configuration.dao;

import fr.cnes.regards.modules.configuration.domain.UIConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Class ILayoutRepository
 * <p>
 * JPA Repository for Layout entities
 *
 * @author Sébastien Binda
 */
public interface IUIConfigurationRepository extends JpaRepository<UIConfiguration, Long> {

    List<UIConfiguration> findByApplicationId(String applicationId);

}
