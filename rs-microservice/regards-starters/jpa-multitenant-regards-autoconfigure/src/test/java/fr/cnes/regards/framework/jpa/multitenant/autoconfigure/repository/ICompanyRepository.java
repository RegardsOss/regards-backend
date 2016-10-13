/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.repository;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.pojo.Company;

/**
 *
 * Class CompanyRepository
 *
 * JPA Company Repository
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface ICompanyRepository extends CrudRepository<Company, Long> {

}
