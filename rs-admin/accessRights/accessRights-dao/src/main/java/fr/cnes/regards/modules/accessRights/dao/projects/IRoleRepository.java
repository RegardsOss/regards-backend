/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.projects;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.accessRights.domain.projects.Role;

public interface IRoleRepository extends CrudRepository<Role, Long> {

    Role findByIsDefault(boolean pIsDefault);

    Role findOneByName(String pBorrowedRoleName);
}
