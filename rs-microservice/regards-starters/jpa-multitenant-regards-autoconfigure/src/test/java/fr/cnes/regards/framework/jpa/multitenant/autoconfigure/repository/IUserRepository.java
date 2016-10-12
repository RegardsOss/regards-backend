/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.repository;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.pojo.User;

/**
 *
 * Class CompanyRepository
 *
 * JPA User Repository
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IUserRepository extends CrudRepository<User, Long> {

}
