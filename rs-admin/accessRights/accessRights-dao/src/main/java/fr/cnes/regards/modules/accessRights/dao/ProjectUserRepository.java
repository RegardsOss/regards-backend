/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

@Repository
public class ProjectUserRepository implements IProjectUserRepository {

    @Override
    public <S extends ProjectUser> S save(S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends ProjectUser> Iterable<S> save(Iterable<S> pEntities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProjectUser findOne(Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(Long pId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<ProjectUser> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<ProjectUser> findAll(Iterable<Long> pIds) {
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
    public void delete(ProjectUser pEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Iterable<? extends ProjectUser> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    @Override
    public ProjectUser findOneByEmail(String pEmail) {
        // TODO Auto-generated method stub
        return null;
    }

}
