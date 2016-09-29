/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

public interface IProjectUserRepository extends CrudRepository<ProjectUser, Long> {

    ProjectUser findOneByLogin(String pLogin);

    boolean exists(String pLogin);
}
