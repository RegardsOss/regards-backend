package fr.cnes.regards.modules.emails.dao;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Empty implementation of {@link IEmailRepository} necessary as long as
 *
 * @author Xavier-Alexandre Brochard
 */
@Repository
public class EmailRepository implements IEmailRepository {

    @Override
    public List<Email> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Email> findAll(final Sort pSort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Email> findAll(final Iterable<Long> pIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Email> List<S> save(final Iterable<S> pEntities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

    @Override
    public <S extends Email> S saveAndFlush(final S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteInBatch(final Iterable<Email> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAllInBatch() {
        // TODO Auto-generated method stub

    }

    @Override
    public Email getOne(final Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Email> List<S> findAll(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Email> List<S> findAll(final Example<S> pExample, final Sort pSort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<Email> findAll(final Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Email> S save(final S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Email findOne(final Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(final Long pId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(final Long pId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(final Email pEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(final Iterable<? extends Email> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    @Override
    public <S extends Email> S findOne(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Email> Page<S> findAll(final Example<S> pExample, final Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends Email> long count(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public <S extends Email> boolean exists(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return false;
    }

}
