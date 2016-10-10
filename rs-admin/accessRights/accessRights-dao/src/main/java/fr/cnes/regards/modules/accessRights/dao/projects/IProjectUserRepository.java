/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.projects;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;

public interface IProjectUserRepository extends CrudRepository<ProjectUser, Long> {

    ProjectUser findOneByEmail(String pLogin);

    boolean exists(String pLogin);
}
