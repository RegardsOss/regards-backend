/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.repository;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.multitenant.pojo.User;

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
