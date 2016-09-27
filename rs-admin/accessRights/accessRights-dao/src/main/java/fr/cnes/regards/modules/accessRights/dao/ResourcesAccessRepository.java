/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;

@Repository
public class ResourcesAccessRepository implements IResourcesAccessRepository {

    @Override
    public <S extends ResourcesAccess> S save(S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends ResourcesAccess> Iterable<S> save(Iterable<S> pEntities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourcesAccess findOne(Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(Long pId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<ResourcesAccess> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<ResourcesAccess> findAll(Iterable<Long> pIds) {
        // TODO Auto-generated method stub
        return null;
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
    public void delete(ResourcesAccess pEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Iterable<? extends ResourcesAccess> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

}
