/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.test.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

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
public class RepositoryStub<T extends IIdentifiable<Long>> implements JpaRepository<T, Long> {

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
    public void flush() {

    }

    @Override
    public <S extends T> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public void deleteInBatch(Iterable<T> entities) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public T getOne(Long aLong) {
        return null;
    }

    @Override
    public <S extends T> S findOne(Example<S> example) {
        return null;
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends T> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends T> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public List<T> findAll() {
        return entities;
    }

    @Override
    public List<T> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public List<T> findAll(final Iterable<Long> pIds) {
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
