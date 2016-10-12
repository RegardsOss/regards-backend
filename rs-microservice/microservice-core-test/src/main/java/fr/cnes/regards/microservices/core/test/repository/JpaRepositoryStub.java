/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test.repository;

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

/**
 * Simple implementation of {@link JpaRepository} for stubbing purposes.
 *
 * @param <T>
 *            The type of entity managed by the repository
 *
 * @author Xavier-Alexandre Brochard
 */
@Repository
@Profile("test")
@Primary
public class JpaRepositoryStub<T extends Identifiable<Long>> implements JpaRepository<T, Long> {

    /**
     * The list of entities managed by the repository, mirroring the data base behaviour.
     */
    private List<T> entities = new ArrayList<>();

    @Override
    public Page<T> findAll(final Pageable pPageable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends T> S save(final S pEntity) {
        entities.removeIf(e -> e.equals(pEntity));
        entities.add(pEntity);
        return pEntity;
    }

    @Override
    public T findOne(final Long pId) {
        return entities.stream().filter(e -> e.getId().equals(pId)).findFirst().get();
    }

    @Override
    public boolean exists(final Long pId) {
        return entities.stream().filter(e -> e.getId().equals(pId)).findAny().isPresent();
    }

    @Override
    public long count() {
        return entities.size();
    }

    @Override
    public void delete(final Long pId) {
        entities.removeIf(e -> e.getId().equals(pId));
    }

    @Override
    public void delete(final T pEntity) {
        entities.remove(pEntity);
    }

    @Override
    public void delete(final Iterable<? extends T> pEntities) {
        for (final T entity : pEntities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        entities = new ArrayList<>();
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
        return entities;
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
        final S saved = save(pEntity);
        flush();
        return saved;
    }

    @Override
    public void deleteInBatch(final Iterable<T> pEntities) {
        final List<T> asCollection = StreamSupport.stream(pEntities.spliterator(), true).collect(Collectors.toList());
        entities.removeAll(asCollection);
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

    /**
     * Get entities
     *
     * @return The list of entities
     */
    protected List<T> getEntities() {
        return entities;
    }

    /**
     * Set entities
     *
     * @param pEntities
     *            The list of entities
     */
    protected void setEntities(final List<T> pEntities) {
        entities = pEntities;
    }

}
