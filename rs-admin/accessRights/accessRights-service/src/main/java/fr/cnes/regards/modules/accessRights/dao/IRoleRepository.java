/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.Role;

@Repository
public interface IRoleRepository extends CrudRepository<Role, Long> {

    @Override
    <S extends Role> List<S> save(Iterable<S> pEntities);

    @Override
    List<Role> findAll();

    @Override
    List<Role> findAll(Iterable<Long> pIds);

    Role findByIsDefault(boolean pIsDefault);
}
