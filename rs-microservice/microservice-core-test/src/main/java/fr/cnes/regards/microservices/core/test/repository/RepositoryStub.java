/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.hateoas.Identifiable;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
@Primary
public class RepositoryStub<T extends Identifiable<Long>> implements CrudRepository<T, Long> {

    protected Set<T> entities = new HashSet<>();

    @Override
    public <S extends T> S save(S pEntity) {
        entities.removeIf(r -> r.equals(pEntity));
        entities.add(pEntity);
        return pEntity;
    }

    @Override
    public T findOne(Long pId) {
        return entities.stream().filter(r -> r.getId().equals(pId)).findFirst().get();
    }

    @Override
    public boolean exists(Long pId) {
        return entities.stream().filter(r -> r.getId().equals(pId)).findAny().isPresent();
    }

    @Override
    public long count() {
        return entities.size();
    }

    @Override
    public void delete(Long pId) {
        entities.removeIf(r -> r.getId().equals(pId));
    }

    @Override
    public void delete(T pEntity) {
        entities.remove(pEntity);
    }

    @Override
    public void delete(Iterable<? extends T> pEntities) {
        for (T entity : pEntities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        entities = new HashSet<>();
    }

    @Override
    public <S extends T> List<S> save(Iterable<S> pEntities) {
        List<S> savedEntities = new ArrayList<>();

        for (S entity : pEntities) {
            savedEntities.add(save(entity));
        }

        return savedEntities;
    }

    @Override
    public Iterable<T> findAll() {
        return entities;
    }

    @Override
    public Iterable<T> findAll(Iterable<Long> pIds) {
        return StreamSupport.stream(pIds.spliterator(), false).map(id -> findOne(id)).collect(Collectors.toList());
    }

}
