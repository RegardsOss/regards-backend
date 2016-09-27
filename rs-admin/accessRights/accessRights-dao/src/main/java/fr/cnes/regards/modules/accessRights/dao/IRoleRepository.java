/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.accessRights.domain.Role;

public interface IRoleRepository extends CrudRepository<Role, Long> {

    Role findByIsDefault(boolean pIsDefault);

    Role findOneByName(String pBorrowedRoleName);
}
