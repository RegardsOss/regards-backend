/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.dataaccess.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.dataaccess.domain.Greeting;

/**
 *
 * TODO Description
 *
 * @author TODO
 *
 */
public interface DaoGreeting extends JpaRepository<Greeting, Long> {

}
