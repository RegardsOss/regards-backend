/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.stub;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobInfo;

/**
 *
 */
@Component
public class JobInfoRepositoryStub extends RepositoryStub<JobInfo> implements IJobInfoRepository {

    @Override
    public List<JobInfo> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JobInfo> findAll(Sort pSort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JobInfo> findAll(Iterable<Long> pIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

    @Override
    public <S extends JobInfo> S saveAndFlush(S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteInBatch(Iterable<JobInfo> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAllInBatch() {
        // TODO Auto-generated method stub

    }

    @Override
    public JobInfo getOne(Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends JobInfo> List<S> findAll(Example<S> pExample) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends JobInfo> List<S> findAll(Example<S> pExample, Sort pSort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<JobInfo> findAll(Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends JobInfo> S findOne(Example<S> pExample) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends JobInfo> Page<S> findAll(Example<S> pExample, Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends JobInfo> long count(Example<S> pExample) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public <S extends JobInfo> boolean exists(Example<S> pExample) {
        // TODO Auto-generated method stub
        return false;
    }

}
