/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.repository;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.transactional.pojo.Company;

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
