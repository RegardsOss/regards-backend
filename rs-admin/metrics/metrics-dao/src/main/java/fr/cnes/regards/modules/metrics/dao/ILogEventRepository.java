/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.metrics.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.metrics.domain.LogEventJpa;

/**
 *
 * @author Christophe Mertz
 */
@Repository
public interface ILogEventRepository extends CrudRepository<LogEventJpa, Long> {

}
