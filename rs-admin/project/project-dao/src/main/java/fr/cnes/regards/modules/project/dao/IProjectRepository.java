/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
public interface IProjectRepository extends JpaRepository<Project, Long> {

    Project findOneByName(String pName);

    Page<Project> findByIsPublicTrue(Pageable pPageable);
}
