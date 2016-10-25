/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.hateoas.Identifiable;

/**
 * Generic implementation a of {@link JpaRepository}.<br>
 * <h1>This is a <strong>stubbed, incorrect and incomplete</strong> implementation, yet wrong! Only use this class for
 * test purposes!</h1>
 *
 * @author CS SI
 */
public class JpaRepositoryStub<T extends Identifiable<Long>> extends RepositoryStub<T>
        implements JpaRepository<T, Long> {

    @Override
    public List<T> findAll() {
        return entities;
    }

    @Override
    public List<T> findAll(final Iterable<Long> pIds) {
        return StreamSupport.stream(pIds.spliterator(), false).map(id -> findOne(id)).collect(Collectors.toList());
    }

    /**
     * Get entities
     *
     * @return The list of entities
     */
    @Override
    protected List<T> getEntities() {
        return entities;
    }

    /**
     * Set entities
     *
     * @param pEntities
     *            The list of entities
     */
    @Override
    protected void setEntities(final List<T> pEntities) {
        entities = pEntities;
    }

    @Override
    public Page<T> findAll(final Pageable pPageable) {
        // Not implemented yet
        return new PageImpl<>(new ArrayList<>());
    }

    @Override
    public <S extends T> S findOne(final Example<S> pExample) {
        // Not implemented yet
        return null;
    }

    @Override
    public <S extends T> Page<S> findAll(final Example<S> pExample, final Pageable pPageable) {
        // Not implemented yet
        return new PageImpl<>(new ArrayList<>());
    }

    @Override
    public <S extends T> long count(final Example<S> pExample) {
        // Not implemented yet
        return 0;
    }

    @Override
    public <S extends T> boolean exists(final Example<S> pExample) {
        // Not implemented yet
        return false;
    }

    @Override
    public List<T> findAll(final Sort pSort) {
        // Not implemented yet
        return new ArrayList<>();
    }

    @Override
    public void flush() {
        // Not implemented yet
    }

    @Override
    public <S extends T> S saveAndFlush(final S pEntity) {
        // Not implemented yet
        return null;
    }

    @Override
    public void deleteInBatch(final Iterable<T> pEntities) {
        // Not implemented yet
    }

    @Override
    public void deleteAllInBatch() {
        // Not implemented yet
    }

    @Override
    public T getOne(final Long pId) {
        // Not implemented yet
        return null;
    }

    @Override
    public <S extends T> List<S> findAll(final Example<S> pExample) {
        // Not implemented yet
        return new ArrayList<>();
    }

    @Override
    public <S extends T> List<S> findAll(final Example<S> pExample, final Sort pSort) {
        // Not implemented yet
        return new ArrayList<>();
    }

}
