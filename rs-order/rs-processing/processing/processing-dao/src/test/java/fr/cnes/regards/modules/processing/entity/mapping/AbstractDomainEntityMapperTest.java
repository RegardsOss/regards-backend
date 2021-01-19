/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.entity.mapping;

import org.junit.Test;

import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomList;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractDomainEntityMapperTest<D, E> {

    abstract DomainEntityMapper<D, E> makeMapper();
    abstract Class<D> domainClass();
    abstract Class<E> entityClass();

    DomainEntityMapper<D, E> mapper = makeMapper();

    @Test
    public void test_map_executions_fromDomain() {
        for (D domain : randomList(domainClass(), 100)) {
            assertThat(mapper.toDomain(mapper.toEntity(domain))).isEqualTo(domain);
        }
    }

    @Test
    public void test_map_executions_fromEntity() {
        for (E entity : randomList(entityClass(), 100)) {
            assertThat(mapper.toEntity(mapper.toDomain(entity))).isEqualTo(entity);
        }
    }


}