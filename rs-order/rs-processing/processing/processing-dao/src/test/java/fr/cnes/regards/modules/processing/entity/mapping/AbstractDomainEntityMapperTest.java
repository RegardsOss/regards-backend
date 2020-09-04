package fr.cnes.regards.modules.processing.entity.mapping;

import org.junit.Test;

import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomList;
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