/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure.repository;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.instance.autoconfigure.pojo.Project;

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
public interface IProjectRepository extends CrudRepository<Project, Long> {

}
