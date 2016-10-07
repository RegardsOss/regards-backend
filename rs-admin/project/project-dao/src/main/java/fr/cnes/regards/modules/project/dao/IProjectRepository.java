/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.domain.annotation.InstanceEntity;
import fr.cnes.regards.modules.project.domain.Project;

@InstanceEntity
public interface IProjectRepository extends CrudRepository<Project, Long> {

    Project findOneByName(String name);
}
