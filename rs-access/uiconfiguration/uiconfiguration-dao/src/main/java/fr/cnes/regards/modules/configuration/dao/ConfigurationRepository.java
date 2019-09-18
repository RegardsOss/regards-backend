package fr.cnes.regards.modules.configuration.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.configuration.domain.Configuration;

/**
*
* Class ILayoutRepository
*
* JPA Repository for Layout entities
*
* @author Sébastien Binda
*/
public interface ConfigurationRepository  extends JpaRepository<Configuration, Long>{

	List<Configuration> findByApplicationId(String applicationId);

}
