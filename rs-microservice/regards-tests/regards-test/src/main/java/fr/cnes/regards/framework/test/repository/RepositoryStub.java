/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 *
 * Class RepositoryStub
 *
 * STUB for all JPA crud repository
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class RepositoryStub<T extends IIdentifiable<Long>> implements CrudRepository<T, Long> {

    /**
     * Internal storage
     */
    protected List<T> entities = new ArrayList<>();

    @Override
    public <S extends T> S save(final S pEntity) {
        entities.removeIf(r -> r.equals(pEntity));
        entities.add(pEntity);
        return pEntity;
    }

    @Override
    public T findOne(final Long pId) {
        return entities.stream().filter(r -> r.getId().equals(pId)).findFirst().orElse(null);
    }

    @Override
    public boolean exists(final Long pId) {
        return entities.stream().filter(r -> r.getId().equals(pId)).findAny().isPresent();
    }

    @Override
    public long count() {
        return entities.size();
    }

    @Override
    public void delete(final Long pId) {
        entities.removeIf(r -> r.getId().equals(pId));
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
    public <S extends T> List<S> save(final Iterable<S> pEntities) {
        final List<S> savedEntities = new ArrayList<>();

        for (final S entity : pEntities) {
            savedEntities.add(save(entity));
        }

        return savedEntities;
    }

    @Override
    public Iterable<T> findAll() {
        return entities;
    }

    @Override
    public Iterable<T> findAll(final Iterable<Long> pIds) {
        try (final Stream<Long> stream = StreamSupport.stream(pIds.spliterator(), false)) {
            return stream.map(id -> findOne(id)).collect(Collectors.toList());
        }
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
