/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.repository.instance;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.microservices.core.dao.annotation.InstanceEntity;
import fr.cnes.regards.microservices.core.dao.pojo.instance.Project;

@InstanceEntity
public interface ProjectRepository extends CrudRepository<Project, Long> {

}
