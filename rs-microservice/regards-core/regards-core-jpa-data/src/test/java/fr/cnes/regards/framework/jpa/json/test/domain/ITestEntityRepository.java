/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json.test.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Repository
public interface ITestEntityRepository extends CrudRepository<TestEntity, Long> {

}
