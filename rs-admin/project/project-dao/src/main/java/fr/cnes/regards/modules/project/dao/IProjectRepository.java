/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class IProjectRepository
 *
 * JPA Repository to access Project entities
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@InstanceEntity
public interface IProjectRepository extends CrudRepository<Project, Long> {

    Project findOneByName(String pName);
}
