/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.repository.projects;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.microservices.core.dao.pojo.projects.User;

/**
 *
 * Class CompanyRepository
 *
 * JPA User Repository
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface UserRepository extends CrudRepository<User, Long> {

}
