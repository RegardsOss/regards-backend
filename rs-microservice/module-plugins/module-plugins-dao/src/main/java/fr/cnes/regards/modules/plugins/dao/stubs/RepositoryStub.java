/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao.stubs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.hateoas.Identifiable;
import org.springframework.stereotype.Repository;

/**
 * TODO description
 * 
 * @author cmertz
 *
 * @param <T>
 */
@Repository
@Profile("test")
@Primary
public class RepositoryStub<T extends Identifiable<Long>> implements CrudRepository<T, Long> {

    /**
     * A set of entities
     */
    protected Set<T> entities = new HashSet<>();

    @Override
    public <S extends T> S save(S pEntity) {
        this.entities.removeIf(r -> r.equals(pEntity));
        this.entities.add(pEntity);
        return pEntity;
    }

    @Override
    public T findOne(Long pId) {
        return this.entities.stream().filter(r -> r.getId().equals(pId)).findFirst().get();
    }

    @Override
    public boolean exists(Long pId) {
        return this.entities.stream().filter(r -> r.getId().equals(pId)).findAny().isPresent();
    }

    @Override
    public long count() {
        return this.entities.size();
    }

    @Override
    public void delete(Long pId) {
        this.entities.removeIf(r -> r.getId().equals(pId));
    }

    @Override
    public void delete(T pEntity) {
        this.entities.remove(pEntity);
    }

    @Override
    public void delete(Iterable<? extends T> pEntities) {
        for (T entity : pEntities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        this.entities = new HashSet<>();
    }

    @Override
    public <S extends T> List<S> save(Iterable<S> pEntities) {
        final List<S> savedEntities = new ArrayList<>();

        for (S entity : pEntities) {
            savedEntities.add(save(entity));
        }

        return savedEntities;
    }

    @Override
    public Iterable<T> findAll() {
        return this.entities;
    }

    @Override
    public Iterable<T> findAll(Iterable<Long> pIds) {
        final Stream<Long> st = StreamSupport.stream(pIds.spliterator(), false);

        final Iterable<T> iter = st.map(id -> findOne(id)).collect(Collectors.toList());
        st.close();

        return iter;
    }

    public Set<T> getEntities() {
        return entities;
    }

    public void setEntities(Set<T> pEntities) {
        this.entities = pEntities;
    }

}
