/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.repository;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.microservices.core.dao.pojo.User;

public interface UserRepository extends CrudRepository<User, Long> {

}
