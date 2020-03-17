package fr.cnes.regards.modules.configuration.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.configuration.domain.UIConfiguration;

/**
*
* Class ILayoutRepository
*
* JPA Repository for Layout entities
*
* @author Sébastien Binda
*/
public interface IUIConfigurationRepository extends JpaRepository<UIConfiguration, Long>{

	List<UIConfiguration> findByApplicationId(String applicationId);

}
