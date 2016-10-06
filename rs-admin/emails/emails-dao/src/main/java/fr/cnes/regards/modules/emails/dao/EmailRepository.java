package fr.cnes.regards.modules.emails.dao;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.emails.domain.EmailDTO;

@Repository
public class EmailRepository implements IEmailRepository {

    @Override
    public List<EmailDTO> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<EmailDTO> findAll(final Sort pSort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<EmailDTO> findAll(final Iterable<Long> pIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends EmailDTO> List<S> save(final Iterable<S> pEntities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

    @Override
    public <S extends EmailDTO> S saveAndFlush(final S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteInBatch(final Iterable<EmailDTO> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAllInBatch() {
        // TODO Auto-generated method stub

    }

    @Override
    public EmailDTO getOne(final Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends EmailDTO> List<S> findAll(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends EmailDTO> List<S> findAll(final Example<S> pExample, final Sort pSort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Page<EmailDTO> findAll(final Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends EmailDTO> S save(final S pEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmailDTO findOne(final Long pId) {
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
    public void delete(final EmailDTO pEntity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(final Iterable<? extends EmailDTO> pEntities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    @Override
    public <S extends EmailDTO> S findOne(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends EmailDTO> Page<S> findAll(final Example<S> pExample, final Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends EmailDTO> long count(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public <S extends EmailDTO> boolean exists(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return false;
    }

}
