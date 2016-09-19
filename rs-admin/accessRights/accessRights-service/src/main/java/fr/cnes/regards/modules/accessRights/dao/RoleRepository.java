package fr.cnes.regards.modules.accessRights.dao;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.Role;

public class RoleRepository implements IRoleRepository {

    @Override
    public <S extends Role> S save(S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Role findOne(Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(Long pId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(Long pId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Role pEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Iterable<? extends Role> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    @Override
    public <S extends Role> List<S> save(Iterable<S> pEntities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Role> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Role> findAll(Iterable<Long> pIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Role findByIsDefault(boolean pIsDefault) {
        // TODO Auto-generated method stub
        return null;
    }

}
