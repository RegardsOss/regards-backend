/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.repository.instance;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.starter.jpa.annotation.InstanceEntity;
import fr.cnes.regards.microservices.core.dao.pojo.instance.Project;

/**
 *
 * Class ProjectRepository
 *
 * JPA Project Repository
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@InstanceEntity
public interface ProjectRepository extends CrudRepository<Project, Long> {

}
