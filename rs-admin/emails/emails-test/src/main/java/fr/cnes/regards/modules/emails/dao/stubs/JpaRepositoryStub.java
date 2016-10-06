/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.dao.stubs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.hateoas.Identifiable;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
@Primary
public class JpaRepositoryStub<T extends Identifiable<Long>> implements JpaRepository<T, Long> {

    protected List<T> entities_ = new ArrayList<>();

    @Override
    public Page<T> findAll(final Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends T> S save(final S pEntity) {
        entities_.removeIf(r -> r.equals(pEntity));
        entities_.add(pEntity);
        return pEntity;
    }

    @Override
    public T findOne(final Long pId) {
        return entities_.stream().filter(r -> r.getId().equals(pId)).findFirst().get();
    }

    @Override
    public boolean exists(final Long pId) {
        return entities_.stream().filter(r -> r.getId().equals(pId)).findAny().isPresent();
    }

    @Override
    public long count() {
        return entities_.size();
    }

    @Override
    public void delete(final Long pId) {
        entities_.removeIf(r -> r.getId().equals(pId));
    }

    @Override
    public void delete(final T pEntity) {
        entities_.remove(pEntity);
    }

    @Override
    public void delete(final Iterable<? extends T> pEntities) {
        for (T entity : pEntities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        entities_ = new ArrayList<>();
    }

    @Override
    public <S extends T> S findOne(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends T> Page<S> findAll(final Example<S> pExample, final Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends T> long count(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public <S extends T> boolean exists(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<T> findAll() {
        return entities_;
    }

    @Override
    public List<T> findAll(final Sort pSort) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<T> findAll(final Iterable<Long> pIds) {
        return StreamSupport.stream(pIds.spliterator(), true).map(id -> findOne(id)).collect(Collectors.toList());
    }

    @Override
    public <S extends T> List<S> save(final Iterable<S> pEntities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

    @Override
    public <S extends T> S saveAndFlush(final S pEntity) {
        S saved = save(pEntity);
        flush();
        return saved;
    }

    @Override
    public void deleteInBatch(final Iterable<T> pEntities) {
        List<T> asCollection = StreamSupport.stream(pEntities.spliterator(), true).collect(Collectors.toList());
        entities_.removeAll(asCollection);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public T getOne(final Long pId) {
        return findOne(pId);
    }

    @Override
    public <S extends T> List<S> findAll(final Example<S> pExample) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends T> List<S> findAll(final Example<S> pExample, final Sort pSort) {
        // TODO Auto-generated method stub
        return null;
    }

}
