package fr.cnes.regards.modules.project.dao;

import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.project.domain.Project;

@Repository
public class ProjectRepository implements IProjectRepository {

    @Override
    public <S extends Project> S save(S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Project> Iterable<S> save(Iterable<S> pEntities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Project findOne(Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(Long pId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<Project> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Project> findAll(Iterable<Long> pIds) {
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
    public void delete(Project pEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(Iterable<? extends Project> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    @Override
    public Project findOneByName(String pName) {
        // TODO Auto-generated method stub
        return null;
    }

}
