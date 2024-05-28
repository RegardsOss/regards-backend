/*
 * Copyright 2016 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.jirutka.spring.data.jdbc;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;
import java.util.List;

/**
 * JDBC specific extension of {@link org.springframework.data.repository.Repository}.
 *
 * @param <T> the domain type the repository manages.
 * @param <ID> the type of the id of the entity the repository manages.
 */
@NoRepositoryBean
public interface JdbcRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {

    List<T> findAll();

    List<T> findAll(Sort sort);

    List<T> findAll(Iterable<ID> ids);

    /**
     * Saves the given entity. If {@link org.springframework.data.domain.Persistable#isNew()
     * entity.isNew()} returns true, then it creates a new record; otherwise it
     * updates the existing one.
     *
     * <p>Use the returned instance for further operations as the save
     * operation might have changed the entity instance completely.</p>
     *
     * @param entity
     * @return A saved entity.
     * @throws IllegalArgumentException if the given entity is null.
     */
    <S extends T> S save(S entity);

    /**
     * Saves all the given entities.
     *
     * @see #save(S)
     * @param entities
     * @return Saved entities.
     * @throws IllegalArgumentException if one of the given entities is null.
     */
    <S extends T> List<S> save(Iterable<S> entities);

    /**
     * Inserts the given new entity into database.
     *
     * <p>Use the returned instance for further operations as the insert
     * operation might have changed the entity instance.</p>
     *
     * @param entity
     * @return An inserted entity.
     * @throws org.springframework.dao.DuplicateKeyException if record with the
     *         same primary key as the given entity already exists.
     */
    <S extends T> S insert(S entity);

    /**
     * Updates the given entity. If no record with the entity's ID exists in
     * the database, then it throws an exception.
     *
     * <p>Use the returned instance for further operations as the update
     * operation might have changed the entity instance.</p>
     *
     * @param entity
     * @return An updated entity.
     * @throws NoRecordUpdatedException if the entity doesn't exist (i.e. no
     *         record has been updated).
     * @throws IllegalArgumentException if some of the properties mapped to the
     *         entity's primary key are null.
     */
    <S extends T> S update(S entity);
}
