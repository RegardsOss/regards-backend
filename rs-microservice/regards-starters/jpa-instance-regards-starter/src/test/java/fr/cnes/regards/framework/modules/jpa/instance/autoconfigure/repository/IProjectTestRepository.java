/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.repository;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.pojo.TestProject;

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
public interface IProjectTestRepository extends CrudRepository<TestProject, Long> {

}
