/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.instance;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.microservices.core.dao.annotation.InstanceEntity;

@InstanceEntity
public interface ProjectRepository extends CrudRepository<Project, Long> {

}
