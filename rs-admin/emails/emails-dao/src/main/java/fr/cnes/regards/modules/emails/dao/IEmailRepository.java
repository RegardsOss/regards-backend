/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.dao;

import org.springframework.data.repository.CrudRepository;

import fr.cnes.regards.modules.emails.domain.Email;

/**
 * Interface for an JPA auto-generated CRUD repository managing Emails
 *
 * @author xbrochard
 */
public interface IEmailRepository extends CrudRepository<Email, Long> {

}
